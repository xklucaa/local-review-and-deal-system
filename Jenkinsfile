pipeline {
    agent any
    tools {
            maven 'Maven'
            jdk 'JDK'
    }
    environment {
        COMPOSE_FILE = 'docker-compose.yml'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Pulling GitHub repository code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo '🛠️ Building project with auto-installed Maven...'
                sh 'mvn clean install -DskipTests'
            }
        }

//         stage('Test') {
//             steps {
//                 sh 'mvn test'
//             }
//         }

        stage('Docker Build & Push') {
            steps {
                echo '🐳 Building images with docker-compose...'
                sh 'docker compose -f ${COMPOSE_FILE} build'
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                echo '🚀 Starting all services...'
                sh 'docker compose -f ${COMPOSE_FILE} up -d'
            }
        }
    }

    post {
        success {
            echo "✅ Build successful and deployed!"
        }
        failure {
            echo "❌ Build failed! Check logs."
        }
    }
}
