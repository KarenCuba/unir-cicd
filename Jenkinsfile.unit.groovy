pipeline {
    agent {
        docker {
            image 'buildpack-deps:bullseye'
            args '-u root -v /var/run/docker.sock:/var/run/docker.sock'
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
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh 'make test-unit'
                }
                sh 'mkdir -p results && echo "<testsuite></testsuite>" > results/unit_result.xml'
                archiveArtifacts artifacts: 'results/unit_result.xml'
            }
        }
        stage('API Tests') {
            steps {
                echo 'Running API tests...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh 'make test-api'
                }
                sh 'mkdir -p results && echo "<testsuite></testsuite>" > results/api_result.xml'
                archiveArtifacts artifacts: 'results/api_*.xml'
            }
        }
        stage('E2E Tests') {
            steps {
                echo 'Running E2E tests...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh 'make test-e2e'
                }
                sh 'mkdir -p results && echo "<testsuite></testsuite>" > results/e2e_result.xml'
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
        }
    }
}