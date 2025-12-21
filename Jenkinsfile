pipeline {
    agent any
    
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = 'farahbali'
        IMAGE_NAME = "${DOCKERHUB_USERNAME}/springpetclinic"
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
        
        stage('Build & Unit Tests') {
            steps {
                echo '🔨 Building project and running unit tests...'
                sh 'mvn clean package -DskipTests=false'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
     stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQube') {
            sh '''
              mvn org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
              -Dsonar.projectKey=springpetclinic \
              -Dsonar.projectName=SpringPetClinic
            '''
        }
    }
}

        
       stage('Start Application for Testing') {
    steps {
        echo '🚀 Starting application for Selenium tests...'
        sh '''
            pkill -f spring-petclinic || true

            nohup java -jar target/*.jar > app.log 2>&1 &
            echo $! > app.pid

            echo "Waiting for application to start..."
            sleep 30

            curl -f http://localhost:8080/login
        '''
    }
}

        
        stage('Selenium Tests') {
       steps {
           echo '🧪 Running Selenium UI tests...'
           sh 'mvn test -Dtest=org.springframework.samples.petclinic.selenium.PetClinicSeleniumTest -DfailIfNoTests=false'
       }
       post {
           always {
               junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
           }
       }
   }
        
        stage('Stop Application') {
            steps {
                echo '🛑 Stopping test application...'
                sh '''
                    if [ -f app.pid ]; then
                        kill $(cat app.pid) || true
                        rm app.pid
                    fi
                    pkill -f 'spring-petclinic' || true
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing image to Docker Hub...'
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                        docker.image("${IMAGE_NAME}:${IMAGE_TAG}").push()
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                echo '☸️ Deploying to Minikube...'
                sh '''
                    # Update deployment file with correct image name
                    sed -i "s|YOUR_DOCKERHUB_USERNAME|${DOCKERHUB_USERNAME}|g" kubernetes/deployment.yaml
                    
                    # Apply Kubernetes configurations
                    kubectl apply -f kubernetes/deployment.yaml
                    kubectl apply -f kubernetes/service.yaml
                    
                    # Wait for deployment to be ready
                    kubectl rollout status deployment/springpetclinic-deployment
                    
                    # Show deployment status
                    kubectl get deployments
                    kubectl get pods
                    kubectl get services
                    
                    # Get the Minikube URL
                    echo "Application URL:"
                    minikube service springpetclinic-service --url
                '''
            }
        }
    }
    
post {
    failure {
        echo '❌ Pipeline failed! Sending notification email...'
        emailext (
            subject: "❌ Jenkins Build Failed: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
            body: """
                <h2>Build Failed</h2>
                <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
                <p><strong>Build URL:</strong>
                <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                <p>Please check the console output for details.</p>
            """,
            to: 'balifarah2001@gmail.com',
            mimeType: 'text/html'
        )
    }

    success {
        echo '✅ Pipeline completed successfully!'
    }

    always {
        echo '🧹 Cleaning up...'
        sh 'docker system prune -f || true'
    }
}

}
