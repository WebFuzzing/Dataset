pipeline {
    agent any

    environment {
        GIT_REPO_URL = 'https://github.com/Rapter1990/flightsearchapi.git'
        BRANCH_NAME = 'main'
        DOCKERHUB_USERNAME = 'noyandocker'
        DOCKER_IMAGE_NAME = 'flightsearchapi-jenkins'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${env.BRANCH_NAME}"]],
                        userRemoteConfigs: [[url: "${env.GIT_REPO_URL}"]]
                    ])
                }
            }
        }

        stage('Build') {
            agent {
                    docker {
                        image 'maven:3.9.9-amazoncorretto-21-alpine'
                    }
                }
            steps {
                sh 'mvn clean install'
            }
        }

        stage('Build Docker Image') {
            agent {
                docker {
                    image 'docker:27.5.1'
                }
            }
            steps {
                sh "docker build -t ${env.DOCKERHUB_USERNAME}/${env.DOCKER_IMAGE_NAME}:latest ."
            }
        }

        stage('Push Docker Image') {
            agent {
                docker {
                    image 'docker:27.5.1'
                }
            }
            steps {
                withDockerRegistry([credentialsId: 'docker-hub-credentials', url: '']) {
                    sh "docker push ${env.DOCKERHUB_USERNAME}/${env.DOCKER_IMAGE_NAME}:latest"
                }
            }
        }

    }

    post {
            always {
                cleanWs(cleanWhenNotBuilt: false,
                        deleteDirs: true,
                        disableDeferredWipeout: true,
                        notFailBuild: true,
                        patterns: [[pattern: '.gitignore', type: 'INCLUDE'],
                                   [pattern: '.propsfile', type: 'EXCLUDE']])
            }
    }
}