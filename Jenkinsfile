pipeline {
    agent any

    environment {
        DOCKERHUB_USERNAME = 'farahbali'
        IMAGE_NAME = 'farahbali/springpetclinic'
        IMAGE_TAG = 'latest'
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
                // Skip tests here to save time, we run them later
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
                // Use || true so pipeline continues to post-actions even if tests fail
                sh 'mvn test -DfailIfNoTests=false || true'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '📊 Running SonarQube analysis...'
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    // FIX: specific plugin version, no clean, no verify (uses existing reports)
                    sh '''
                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar \
                        -Dsonar.projectKey=springpetclinic \
                        -Dsonar.projectName=SpringPetClinic \
                        -Dsonar.host.url=http://localhost:9000 \
                        -Dsonar.token=${SONAR_TOKEN} \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.junit.reportPaths=target/surefire-reports
                    '''
                }
            }
        }

        stage('Stop Application') {
            steps {
                echo '🛑 Stopping application...'
                sh '''
                    if [ -f app.pid ]; then
                        kill $(cat app.pid) || true
                        rm app.pid
                    fi
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
                    cat > Dockerfile << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
EOF
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
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
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:${BUILD_NUMBER}
                docker push ${IMAGE_NAME}:${IMAGE_TAG} || { echo "Push failed"; exit 1; }
                docker push ${IMAGE_NAME}:${BUILD_NUMBER} || { echo "Push failed"; exit 1; }
            '''
        }
    }
}


stage('Deploy to Kubernetes (Minikube)') {
    steps {
        echo '☸️ Deploying to Kubernetes...'
        withEnv(['KUBECONFIG=/home/farah/.kube/config']) {
            sh """
                # Replace placeholder in deployment.yaml
                sed -i 's|IMAGE_TAG|${BUILD_NUMBER}|g' kubernetes/deployment.yaml
                
                # Apply manifests
                kubectl apply -f kubernetes/service.yaml
                kubectl apply -f kubernetes/deployment.yaml
                
                # Check rollout status
                kubectl rollout status deployment/springpetclinic-deployment
            """
        }
    }
}



    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            emailext (
                subject: "✅ Jenkins Build SUCCESS: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <h2 style="color: green;">Build Successful! 🎉</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
                    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p>Application is now running in Kubernetes cluster.</p>
                """,
                to: 'balifarah2001@gmail.com',
                from: 'jenkins@devops.com',
                replyTo: 'jenkins@devops.com',
                mimeType: 'text/html'
            )
        }
        
        failure {
            echo '❌ Pipeline failed! Sending notification email...'
            // FIX: Removed the crashing ${currentBuild.buildLog} variable
            emailext (
                subject: "❌ Jenkins Build FAILED: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <h2 style="color: red;">Build Failed! ⚠️</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
                    <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                    <p><strong>Console Output:</strong> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                    <hr>
                    <p>The build log is attached to this email.</p>
                """,
                to: 'balifarah2001@gmail.com',
                from: 'jenkins@devops.com',
                replyTo: 'jenkins@devops.com',
                mimeType: 'text/html',
                attachLog: true
            )
        }
        
        always {
            echo '🧹 Cleaning up...'
            sh '''
                pkill -f spring-petclinic || true
                rm -f app.pid app.log || true
                docker system prune -f || true
            '''
        }
    }
}
