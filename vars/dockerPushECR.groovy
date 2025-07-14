def call(Map config = [:]) {
    def imageName = config.imageName ?: error("Image name is required")
    def imageTag = config.imageTag ?: 'latest'
    def awsRegion = config.awsRegion ?: 'us-east-1'
    def ecrRepo = config.ecrRepo ?: error("ECR repository is required")
    def awsCredentials = config.credentials ?: 'aws-ecr-credentials'

    def ecrImage = "${ecrRepo}:${imageTag}"

    echo "Pushing Docker image to ECR: ${ecrImage}"

    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentials]]) {
        sh """
            set -e
            echo "Logging in to AWS ECR registry ${ecrRepo.split('/')[0]}"
            aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${ecrRepo.split('/')[0]}
            
            echo "Tagging image ${imageName}:${imageTag} as ${ecrImage}"
            docker tag ${imageName}:${imageTag} ${ecrImage}
            
            echo "Pushing image to ECR..."
            docker push ${ecrImage}
        """
    }
}
