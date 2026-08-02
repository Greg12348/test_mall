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
                        credentialsId: 'dockerhub-credential',
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

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'mall-kubeconfig',
                        variable: 'KUBECONFIG_FILE'
                    )
                ]) {
                    bat '''
                        @echo off
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" set image deployment/product-service product-service=%PRODUCT_IMAGE%:%IMAGE_TAG% -n mall
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" set image deployment/order-service order-service=%ORDER_IMAGE%:%IMAGE_TAG% -n mall
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" set image deployment/api-gateway api-gateway=%GATEWAY_IMAGE%:%IMAGE_TAG% -n mall

                        kubectl --kubeconfig "%KUBECONFIG_FILE%" rollout status deployment/product-service -n mall --timeout=180s
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" rollout status deployment/order-service -n mall --timeout=180s
                        kubectl --kubeconfig "%KUBECONFIG_FILE%" rollout status deployment/api-gateway -n mall --timeout=180s
                    '''
                }
            }
        }

        stage('Gateway Smoke Test') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'mall-kubeconfig',
                        variable: 'KUBECONFIG_FILE'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"
                        $env:KUBECONFIG = $env:KUBECONFIG_FILE

                        $portForward = Start-Process `
                            -FilePath "kubectl" `
                            -ArgumentList @(
                                "port-forward", "service/api-gateway",
                                "18080:8080", "-n", "mall"
                            ) `
                            -WindowStyle Hidden `
                            -PassThru

                        try {
                            $env:GATEWAY_URL = "http://localhost:18080"
                            & ".\\system-tests\\successful-order-gateway.ps1"

                            if ($LASTEXITCODE -ne 0) {
                                throw "Gateway smoke test failed with exit code $LASTEXITCODE."
                            }
                        }
                        finally {
                            Stop-Process -Id $portForward.Id -Force -ErrorAction SilentlyContinue
                        }
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
