pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        DOCKER_NAMESPACE = 'greg12348'
        PRODUCT_IMAGE = "${DOCKER_NAMESPACE}/mall-product-service"
        ORDER_IMAGE = "${DOCKER_NAMESPACE}/mall-order-service"
        GATEWAY_IMAGE = "${DOCKER_NAMESPACE}/mall-api-gateway"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                bat '''
                    @echo off
                    call mvn -B -pl product-service,order-service,api-gateway verify
                '''
            }
        }

        stage('Build Docker Images') {
            steps {
                bat '''
                    @echo off
                    docker build -f product-service/Dockerfile -t %PRODUCT_IMAGE%:%IMAGE_TAG% .
                    docker build -f order-service/Dockerfile -t %ORDER_IMAGE%:%IMAGE_TAG% .
                    docker build -f api-gateway/Dockerfile -t %GATEWAY_IMAGE%:%IMAGE_TAG% .
                '''
            }
        }

        stage('Push Docker Images') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_TOKEN'
                    )
                ]) {
                    bat '''
                        @echo off
                        echo %DOCKER_TOKEN% | docker login --username %DOCKER_USERNAME% --password-stdin
                        docker push %PRODUCT_IMAGE%:%IMAGE_TAG%
                        docker push %ORDER_IMAGE%:%IMAGE_TAG%
                        docker push %GATEWAY_IMAGE%:%IMAGE_TAG%
                        docker logout
                    '''
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
        }
    }
}
