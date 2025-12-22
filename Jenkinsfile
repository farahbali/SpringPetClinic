pipeline {
    agent any

    environment {
        DOCKERHUB_USERNAME = 'farahbali'
        IMAGE_NAME = 'farahbali/springpetclinic'
        IMAGE_TAG = 'latest'
        DOCKER_REGISTRY = 'docker.io'
    }

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Checking out code from GitHub...'
                git branch: 'master',
                    url: 'https://github.com/farahbali/SpringPetClinic.git'
            }
        }

        stage('Build Application') {
            steps {
                echo '🧱 Building Spring Boot application...'
                sh 'mvn clean package -DskipTests'
            }
        }

       stage('Start Application') {
            steps {
                echo '🚀 Starting application...'
                sh '''
                    pkill -f spring-petclinic || true

                    nohup java -jar target/*.jar > app.log 2>&1 &
                    echo $! > app.pid

                    echo "Waiting for application to start..."
                    sleep 30

                    curl -I http://localhost:8080 || exit 1
                '''
            }
        }

        stage('Run Tests') {
            steps {
                echo '🧪 Running tests...'
                sh 'mvn test -DfailIfNoTests=false'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube analysis...'
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
                        -Dsonar.projectKey=springpetclinic \
                        -Dsonar.projectName=SpringPetClinic \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_AUTH_TOKEN}
                    '''
                }
            }
        }

        stage('Stop Application') {
            steps {
                echo '🛑 Stopping application...'
                sh '''
                    if [ -f app.pid ]; then
                        PID=$(cat app.pid)
                        echo "Found PID: $PID"
                        if kill -0 $PID 2>/dev/null; then
                            echo "Stopping process $PID gracefully..."
                            kill $PID
                            
                            # Wait for graceful shutdown
                            for i in {1..10}; do
                                if ! kill -0 $PID 2>/dev/null; then
                                    echo "Process stopped gracefully"
                                    break
                                fi
                                sleep 1
                            done
                            
                            # Force kill if still running
                            if kill -0 $PID 2>/dev/null; then
                                echo "Force killing process $PID..."
                                kill -9 $PID
                            fi
                        else
                            echo "Process $PID not running"
                        fi
                        rm -f app.pid
                    else
                        echo "No PID file found, trying to kill by name..."
                        pkill -f spring-petclinic || true
                    fi
                    
                    # Clean up log file
                    rm -f app.log 2>/dev/null || true
                    
                    # Verify port is free
                    sleep 2
                    if netstat -tuln | grep :8080 > /dev/null; then
                        echo "Warning: Port 8080 is still in use"
                        fuser -k 8080/tcp || true
                    fi
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
                    # Create Dockerfile if not exists
                    if [ ! -f Dockerfile ]; then
                        cat > Dockerfile << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
EOF
                    fi
                    
                    # Build with buildkit for better performance
                    DOCKER_BUILDKIT=1 docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    
                    # Also tag with build number
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:${BUILD_NUMBER}
                    
                    echo "✅ Docker image built successfully"
                    docker images | grep ${IMAGE_NAME}
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing image to Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "Logging in to Docker Hub..."
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        
                        echo "Pushing image ${IMAGE_NAME}:${IMAGE_TAG}..."
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        
                        echo "Pushing image ${IMAGE_NAME}:${BUILD_NUMBER}..."
                        docker push ${IMAGE_NAME}:${BUILD_NUMBER}
                        
                        echo "✅ Images pushed successfully to Docker Hub"
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes (Minikube)') {
            steps {
                echo '☸️ Deploying to Kubernetes...'
                sh '''
                    # Verify Minikube is running
                    if ! minikube status | grep -q "Running"; then
                        echo "❌ Minikube is not running. Starting Minikube..."
                        minikube start || { echo "Failed to start Minikube"; exit 1; }
                    fi
                    
                    # Set Minikube docker env if needed
                    eval $(minikube docker-env 2>/dev/null) || true
                    
                    echo "Applying Kubernetes manifests..."
                    # Create namespace if it doesn't exist
                    kubectl create namespace spring-petclinic 2>/dev/null || true
                    
                    # Deploy with namespace
                    kubectl apply -f kubernetes/deployment.yaml -n spring-petclinic
                    kubectl apply -f kubernetes/service.yaml -n spring-petclinic
                    
                    echo "Waiting for deployment to be ready..."
                    kubectl rollout status deployment/springpetclinic-deployment -n spring-petclinic --timeout=300s
                    
                    echo "✅ Deployment completed"
                    kubectl get pods -n spring-petclinic -o wide
                    kubectl get svc -n spring-petclinic -o wide
                '''
            }
        }

        stage('Verify Minikube Deployment') {
            steps {
                echo '✅ Verifying application on Minikube...'
                sh '''
                    # Get Minikube IP
                    MINIKUBE_IP=$(minikube ip)
                    echo "Minikube IP: $MINIKUBE_IP"
                    
                    # Get NodePort
                    NODE_PORT=$(kubectl get svc springpetclinic-service -n spring-petclinic -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null)
                    
                    if [ -z "$NODE_PORT" ]; then
                        echo "❌ Could not get NodePort for service"
                        kubectl describe svc springpetclinic-service -n spring-petclinic
                        exit 1
                    fi
                    
                    echo "Service NodePort: $NODE_PORT"
                    echo "Testing application at http://$MINIKUBE_IP:$NODE_PORT"
                    
                    # Test application health with retries
                    MAX_RETRIES=10
                    RETRY_INTERVAL=10
                    
                    for i in $(seq 1 $MAX_RETRIES); do
                        echo "Health check attempt $i/$MAX_RETRIES..."
                        
                        if curl -s -f "http://$MINIKUBE_IP:$NODE_PORT/actuator/health" > /dev/null 2>&1; then
                            echo "✅ Application health check passed!"
                            curl -s "http://$MINIKUBE_IP:$NODE_PORT/actuator/health" | python -m json.tool 2>/dev/null || \
                            curl -s "http://$MINIKUBE_IP:$NODE_PORT/actuator/health"
                            break
                        fi
                        
                        if curl -s -f "http://$MINIKUBE_IP:$NODE_PORT" > /dev/null 2>&1; then
                            echo "✅ Application is responding!"
                            break
                        fi
                        
                        if [ $i -eq $MAX_RETRIES ]; then
                            echo "❌ Application failed to respond after $MAX_RETRIES attempts"
                            echo "Pod logs:"
                            kubectl logs deployment/springpetclinic-deployment -n spring-petclinic --tail=50
                            echo "Pod description:"
                            kubectl describe pods -l app=springpetclinic -n spring-petclinic
                            exit 1
                        fi
                        
                        sleep $RETRY_INTERVAL
                    done
                    
                    echo "✅ Application is successfully running on Minikube!"
                    echo "🌐 Access URL: http://$MINIKUBE_IP:$NODE_PORT"
                    
                    # Alternative: Use minikube service command
                    echo "Alternative access method:"
                    minikube service springpetclinic-service -n spring-petclinic --url 2>/dev/null || true
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            script {
                def minikube_ip = sh(script: 'minikube ip 2>/dev/null || echo "localhost"', returnStdout: true).trim()
                def node_port = sh(script: 'kubectl get svc springpetclinic-service -n spring-petclinic -o jsonpath=\'{.spec.ports[0].nodePort}\' 2>/dev/null || echo "N/A"', returnStdout: true).trim()
                
                emailext(
                    subject: "✅ Jenkins Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                    <h2>Build Successful! 🎉</h2>
                    <p><b>Job:</b> ${env.JOB_NAME}</p>
                    <p><b>Build Number:</b> ${env.BUILD_NUMBER}</p>
                    <p><b>Status:</b> <span style="color: green; font-weight: bold;">SUCCESS</span></p>
                    <p><b>Build URL:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p><b>Application URL:</b> <a href="http://${minikube_ip}:${node_port}">http://${minikube_ip}:${node_port}</a></p>
                    <p><b>Docker Image:</b> ${env.IMAGE_NAME}:${env.IMAGE_TAG}</p>
                    <p><b>Pipeline Duration:</b> ${currentBuild.durationString}</p>
                    <hr>
                    <p>Application deployed successfully to Minikube Kubernetes cluster.</p>
                    """,
                    to: 'balifarah2001@gmail.com',
                    mimeType: 'text/html'
                )
            }
        }
        
        failure {
            echo '❌ Pipeline failed – sending email notification...'
            script {
                def failedStage = currentBuild.result
                def duration = currentBuild.durationString
                
                emailext(
                    subject: "❌ Jenkins Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                    <h2>Build Failed ⚠️</h2>
                    <p><b>Job:</b> ${env.JOB_NAME}</p>
                    <p><b>Build Number:</b> ${env.BUILD_NUMBER}</p>
                    <p><b>Status:</b> <span style="color: red; font-weight: bold;">FAILED</span></p>
                    <p><b>Build URL:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                    <p><b>Pipeline Duration:</b> ${duration}</p>
                    <p><b>Failed Stage:</b> ${failedStage}</p>
                    <hr>
                    <p>Please check the Jenkins console output for details.</p>
                    <p>To investigate:</p>
                    <ol>
                        <li>Check the failed stage in the pipeline</li>
                        <li>Review application logs if available</li>
                        <li>Verify Minikube/Kubernetes cluster status</li>
                        <li>Check Docker image build and push logs</li>
                    </ol>
                    <p><a href="${env.BUILD_URL}console">Click here to view the full build log</a></p>
                    """,
                    to: 'balifarah2001@gmail.com',
                    mimeType: 'text/html',
                    attachLog: true,  // This attaches the full build log
                    compressLog: true
                )
            }
        }
        
        unstable {
            echo '⚠️ Pipeline unstable – sending email notification...'
            emailext(
                subject: "⚠️ Jenkins Build UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                <h2>Build Unstable ⚠️</h2>
                <p><b>Job:</b> ${env.JOB_NAME}</p>
                <p><b>Build:</b> ${env.BUILD_NUMBER}</p>
                <p><b>URL:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                <p><b>Status:</b> UNSTABLE (Tests may have failed but pipeline continued)</p>
                """,
                to: 'balifarah2001@gmail.com',
                mimeType: 'text/html'
            )
        }
        
        always {
            echo '🧹 Cleaning up resources...'
            sh '''
                # Clean Docker resources
                echo "Cleaning Docker resources..."
                docker system prune -f || true
                
                # Stop application if still running (safety check)
                if [ -f app.pid ]; then
                    PID=$(cat app.pid 2>/dev/null)
                    kill $PID 2>/dev/null || true
                    rm -f app.pid app.log 2>/dev/null || true
                fi
                
                # Kill any remaining spring processes
                pkill -f spring-petclinic || true
                
                echo "Cleanup completed"
            '''
            
            // Clean workspace (optional)
            cleanWs()
        }
    }
}
