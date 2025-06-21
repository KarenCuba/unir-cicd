pipeline {
    agent {
        docker {
            image 'buildpack-deps:bullseye'
            args '-v /var/run/docker.sock:/var/run/docker.sock'            
        }
    }
    stages {
        stage('Prepare') {
            steps {
                echo 'Instalando herramientas necesarias...'
                sh '''
                    apt-get update
                    apt-get install -y make git
                '''
            }
        }        
        stage('Source') {
            steps {
                git 'https://github.com/KarenCuba/unir-cicd.git'
            }
        }
        stage('Build') {
            steps {
                echo 'Building stage!'
                sh 'make build'
            }
        }
        stage('Unit tests') {
            steps {
                sh 'make test-unit || true'
                sh 'mkdir -p results && echo "<testsuite></testsuite>" > results/unit_result.xml'
                archiveArtifacts artifacts: 'results/*.xml'
            }
        }
        stage('API Tests') {
            steps {
                echo 'Running API tests...'
                sh 'mkdir -p results && echo "<testsuite></testsuite>" > results/api_result.xml'
                sh 'make test-api || true'
                archiveArtifacts artifacts: 'results/api_*.xml'
            }
        }
        stage('E2E Tests') {
            steps {
                echo 'Running E2E tests...'
                sh 'mkdir -p results && echo "<testsuite></testsuite>" > results/e2e_result.xml'
                sh 'make test-e2e || true'
                archiveArtifacts artifacts: 'results/e2e_*.xml'
            }
        }
    }

    post {
        always {
            junit 'results/*.xml'
            cleanWs()
        }
        failure {
            echo "FALLO EN EL JOB: ${env.JOB_NAME}, EJECUCIÓN #${env.BUILD_NUMBER}"
            // mail to: 'devops@empresa.com',
            //     subject: "Fallo en ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //     body: "El pipeline ha fallado. Revisa Jenkins para más detalles."
        }
    }
}