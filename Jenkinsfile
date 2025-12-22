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
                sh '''
                    mvn clean package -DskipTests
                '''
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
                sh '''
                    mvn test -DfailIfNoTests=false || true
                '''
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
            sh '''
                mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar \
                -Dsonar.projectKey=springpetclinic \
                -Dsonar.projectName=SpringPetClinic \
                -Dsonar.host.url=http://localhost:9000 \
                -Dsonar.token=${SONAR_TOKEN} \
                -Dsonar.java.binaries=target/classes
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
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes (Minikube)') {
            steps {
                echo '☸️ Deploying to Kubernetes...'
                sh '''
                    # IMPORTANT: Minikube must already be running
                    kubectl apply -f kubernetes/deployment.yaml
                    kubectl apply -f kubernetes/service.yaml

                    kubectl rollout status deployment/springpetclinic-deployment
                    kubectl get pods
                    kubectl get services
                '''
            }
        }
    
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed. Check logs.'
        }
    }
}
