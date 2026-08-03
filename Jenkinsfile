pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '753974169033'
        EKS_CLUSTER = 'mall-test'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        PRODUCT_IMAGE = "${ECR_REGISTRY}/mall-product-service"
        ORDER_IMAGE = "${ECR_REGISTRY}/mall-order-service"
        GATEWAY_IMAGE = "${ECR_REGISTRY}/mall-api-gateway"
        KUBECONFIG = "${WORKSPACE}\\.kube\\config"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                }
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

        stage('Build ECR Images') {
            steps {
                bat '''
                    @echo off
                    docker build -f product-service/Dockerfile -t %PRODUCT_IMAGE%:%IMAGE_TAG% .
                    docker build -f order-service/Dockerfile -t %ORDER_IMAGE%:%IMAGE_TAG% .
                    docker build -f api-gateway/Dockerfile -t %GATEWAY_IMAGE%:%IMAGE_TAG% .
                '''
            }
        }

        stage('Push ECR Images') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-mall-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    bat '''
                        @echo off
                        cmd /c "aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ECR_REGISTRY%"
                        docker push %PRODUCT_IMAGE%:%IMAGE_TAG%
                        docker push %ORDER_IMAGE%:%IMAGE_TAG%
                        docker push %GATEWAY_IMAGE%:%IMAGE_TAG%
                        docker logout %ECR_REGISTRY%
                    '''
                }
            }
        }

        stage('Deploy to EKS') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-mall-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        New-Item -ItemType Directory -Path (Split-Path $env:KUBECONFIG) -Force | Out-Null
                        aws eks update-kubeconfig `
                            --name $env:EKS_CLUSTER `
                            --region $env:AWS_REGION `
                            --kubeconfig $env:KUBECONFIG

                        $kustomizationPath = ".\\aws\\kubernetes\\kustomization.yaml"
                        $kustomization = Get-Content $kustomizationPath -Raw
                        $kustomization = $kustomization -replace 'newTag: aws-test-1', "newTag: $env:IMAGE_TAG"
                        Set-Content -Path $kustomizationPath -Value $kustomization -Encoding utf8

                        kubectl apply -k .\\aws\\kubernetes
                        if ($LASTEXITCODE -ne 0) {
                            throw "EKS manifest deployment failed."
                        }

                        foreach ($deployment in @("product-service", "order-service", "api-gateway")) {
                            kubectl rollout status "deployment/$deployment" -n mall --timeout=300s
                            if ($LASTEXITCODE -ne 0) {
                                throw "Rollout failed for $deployment."
                            }
                        }
                    '''
                }
            }
        }

        stage('AWS Gateway System Test') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-mall-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"
                        $stdoutPath = Join-Path $env:TEMP "mall-jenkins-port-forward-$env:BUILD_NUMBER.out.log"
                        $stderrPath = Join-Path $env:TEMP "mall-jenkins-port-forward-$env:BUILD_NUMBER.err.log"

                        $portForward = Start-Process `
                            -FilePath "kubectl" `
                            -ArgumentList @(
                                "port-forward", "service/api-gateway",
                                "18080:8080", "-n", "mall"
                            ) `
                            -WindowStyle Hidden `
                            -RedirectStandardOutput $stdoutPath `
                            -RedirectStandardError $stderrPath `
                            -PassThru

                        try {
                            $env:GATEWAY_URL = "http://127.0.0.1:18080"
                            $powerShellPath = (Get-Process -Id $PID).Path
                            & $powerShellPath `
                                -NoProfile `
                                -ExecutionPolicy Bypass `
                                -File ".\\system-tests\\successful-order-gateway.ps1"

                            if ($LASTEXITCODE -ne 0) {
                                throw "AWS Gateway system test failed with exit code $LASTEXITCODE."
                            }
                        }
                        finally {
                            if (-not $portForward.HasExited) {
                                Stop-Process -Id $portForward.Id -Force -ErrorAction SilentlyContinue
                                $portForward.WaitForExit()
                            }
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
