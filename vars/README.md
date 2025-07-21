# Jenkins Shared Library Files

## Example Jenkinsfile using shared library

```bash
@Library('jenkins-shared-library') _ // Load your shared library

def deployApproved = false // Declare at top level for pipeline-wide scope

pipeline {
    agent any

    tools {
        nodejs 'nodejs23'
    }

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        IMAGE_NAME = "dashboard-app" // replace with your image name
        IMAGE_TAG = "v${env.BUILD_NUMBER}"
        VERSION = "1.0.${env.BUILD_NUMBER}"
        AWS_REGION = 'us-west-2' // replace with your AWS region
        AWS_ACCOUNT_ID = '123456789012' // replace with your AWS account ID
    }

    options {
        disableResume()
        disableConcurrentBuilds abortPrevious: true 
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        // Clonning the repository
        stage('Git Checkout') {
            steps {
                checkoutPrivRepo(
                    'https://github.com/your-org/repo.git',             // change with your repo and branch
                    'main', 
                    'github-credentials-id'
                )  
            }
        }

        // Checking out the syntax of the JS files
        stage('Check JS Syantax') {
            steps {
                checkJsSyntax()
            }
        }

        // stage('GitLeaks Scan') {
        //     steps {
        //         gitleaksScan()
        //     }
        // }

        // Dependencies and Vulnerability Scanning
        stage('SCA-Dependency Scanning') {
            parallel {
                stage('NPM Audit') {
                    steps {
                        npmAudit()
                    }
                }
                stage('OWASP Scan') {
                    steps {
                        owaspDependencyCheck()
                    }
                }
            }
        }

        // SAST Analysis
        stage('SAST-SonarQube Analysis') {
            steps {

                sonarAnalysis('my-project-key', 'My Project', env.VERSION) // replace with your project key and version
            }
        }

        stage('Quality Gate Check') {
            steps {
                qualityGateCheck('sonar-token')
            }
        }

        // Building the Docker image
        stage('Docker Build') {
            steps {
                dockerBuild(
                    imageName: 'dashboard-app', //replace with your docker image name
                    imageTag: env.IMAGE_TAG,
                    dockerfile: 'Dockerfile',
                    context: '.'
                )
            }
        }

        //Scanning the Docker image with Trivy
        stage('Trivy Image Scan') {
            steps {
                trivyImageScan(
                    imageName: env.IMAGE_NAME,
                    imageTag: env.IMAGE_TAG,
                    outputDir: 'trivy-reports',
                    failOnCritical: true
                )
            }
        }

        // stage('Archive Trivy Reports') {
        //     steps {
        //         archiveArtifacts artifacts: 'trivy-reports/*.html', allowEmptyArchive: true
        //     }
        // }

        // Pushing the Docker image to ECR
        stage('Docker Push - ECR') {
            steps {
                dockerPushECR(
                    imageName: env.IMAGE_NAME,
                    imageTag: env.IMAGE_TAG,
                    awsRegion: env.AWS_REGION,
                    ecrRepo: "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/${env.IMAGE_NAME}", // replace with your ECR repository
                    dockerfile: 'Dockerfile',
                    credentials: 'aws-ecr-credentials'
                )
            }
        }

        // Approval for deployment to specific environment
        stage('Approval for Deployment') {
            when {
                allOf {
                    anyOf {
                        branch 'staging'
                        branch 'main'
                    }
                    expression { return !env.CHANGE_ID }  // Skip PRs
                }
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'staging') {
                        echo "Auto-approving deployment for staging branch."
                        deployApproved = true
                    } else if (env.BRANCH_NAME == 'main') {
                        try {
                            timeout(time: 7, unit: 'DAYS') {
                                def response = input(
                                    message: 'Deploy to production cluster?',
                                    parameters: [
                                        choice(name: 'Proceed?', choices: ['Yes', 'No'], description: 'Choose Yes to deploy')
                                    ]
                                )
                                if (response == 'Yes') {
                                    deployApproved = true
                                } else {
                                    echo "Deployment not approved. Aborting."
                                    currentBuild.result = 'ABORTED'
                                    error("User aborted deployment.")
                                }
                            }
                        } catch (org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution$TimeoutException e) {
                            echo "Approval timed out. Aborting pipeline."
                            currentBuild.result = 'ABORTED'
                            error("No approval received within timeout period.")
                        }
                    } else {
                        // For safety, don't deploy for other branches
                        deployApproved = false
                        echo "Deployment not allowed on branch ${env.BRANCH_NAME}"
                    }
                }
            }
        }

        // Change image tag in GitOps repository and deploy to cluster
        stage('Deploy to Cluster') {
            when {
                expression { return deployApproved }
            }
            steps {
                script {
                    // Define environment based on branch
                    def targetEnvironment = (env.BRANCH_NAME == 'main') ? 'prod' : 'staging'
                    def gitopsBranch = (targetEnvironment == 'prod') ? 'main' : 'staging'

                    echo "Deploying to ${targetEnvironment} environment..."

                    changeImageTag(
                        imageTag: env.IMAGE_TAG,
                        ecrRepo: "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com",
                        manifestsPath: 'kubernetes',
                        repoUrl: 'https://github.com/your-org/your-gitops-repo.git',
                        gitCredentials: 'github-credentials-id',
                        branch: gitopsBranch,
                        gitUserName: 'Jenkins CI',
                        gitUserEmail: 'jenkins@example.com'
                    )
                }
            }
        }

        // This stage will wait for user input to confirm if ArgoCD has synced the deployment
        stage('Confirm ArgoCD Sync') {
            when {
                allOf {
                    anyOf {
                        branch 'staging'
                        branch 'main'
                    }
                    expression { return !env.CHANGE_ID } // skip PRs
                }
            }
            steps {
                script {
                    timeout(time: 3, unit: 'DAYS') {
                        def userInput = input(
                            id: 'ConfirmArgoCDSync', message: 'Has ArgoCD synced the deployment?', parameters: [
                                [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Check if ArgoCD is synced', name: 'IsSynced']
                            ]
                        )
                        if (!userInput) {
                            error "ArgoCD sync not confirmed, aborting pipeline."
                        }
                        echo "User confirmed ArgoCD is synced, proceeding..."
                    }
                }
            }
        }
    }
    post {
        success {
            echo "Pipeline completed successfully."
        }
        failure {
            echo "Pipeline failed. Please check the logs for details."
        }
    }
}
```
## OWASP ZAP Scan
```bash
stage('DAST - Quick ZAP Baseline Scan') {
    when {
        allOf {
            anyOf {
                branch 'staging'
                branch 'main'
            }
            expression { return !env.CHANGE_ID } // Skip PRs
        }
    }
    steps {
        script {
            def targetURL = (env.BRANCH_NAME == 'main') ? 'https://prod.example.com' : 'https://staging.example.com'

            echo "Running quick ZAP baseline scan against: ${targetURL}"

            sh """
            docker run --rm -t owasp/zap2docker-stable zap-baseline.py \\
              -t ${targetURL} \\
              -r zap-baseline-report.html \\
              -j \\
              --exit-code 0 \\
              --quick-scan
            """

            archiveArtifacts artifacts: 'zap-baseline-report.html', allowEmptyArchive: true
        }
    }
}
```
