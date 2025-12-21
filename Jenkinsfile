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

        /* ===================== */
        /* 1️⃣ CHECKOUT SOURCE   */
        /* ===================== */
        stage('Checkout') {
            steps {
                echo '📥 Cloning GitHub repository...'
                git branch: 'master',
                    url: 'https://github.com/farahbali/SpringPetClinic.git'
            }
        }

        /* ===================== */
        /* 2️⃣ BUILD APPLICATION */
        /* ===================== */
        stage('Build') {
            steps {
                echo '🧱 Building Spring Boot application...'
                sh 'mvn clean package -DskipTests'
            }
        }

        /* ===================== */
        /* 3️⃣ RUN TESTS         */
        /* ===================== */
        stage('Tests') {
            steps {
                echo '🧪 Running unit tests...'
                sh 'mvn test -DfailIfNoTests=false'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        /* ===================== */
        /* 4️⃣ SONARQUBE         */
        /* ===================== */
        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube analysis...'
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                        -Dsonar.projectKey=springpetclinic \
                        -Dsonar.projectName=SpringPetClinic
                    '''
                }
            }
        }

        /* ===================== */
        /* 5️⃣ DOCKER BUILD      */
        /* ===================== */
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

        /* ===================== */
        /* 6️⃣ DOCKER PUSH       */
        /* ===================== */
        stage('Push Docker Image') {
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

        /* ===================== */
        /* 7️⃣ KUBERNETES DEPLOY */
        /* ===================== */
        stage('Deploy to Kubernetes (Minikube)') {
            steps {
                echo '☸️ Deploying to Kubernetes...'
                sh '''
                    # Start minikube if not running
                    minikube status || minikube start --driver=docker

                    # Apply Kubernetes manifests
                    kubectl apply -f kubernetes/deployment.yaml
                    kubectl apply -f kubernetes/service.yaml

                    # Wait for deployment
                    kubectl rollout status deployment/springpetclinic-deployment

                    # Display status
                    kubectl get pods
                    kubectl get services
                '''
            }
        }
    }

    post {
        success {
            echo '✅ FULL CI/CD PIPELINE SUCCESS (BUILD → TEST → SONAR → DOCKER → K8S)'
        }
        failure {
            echo '❌ Pipeline failed – check Jenkins console output'
        }
    }
}
