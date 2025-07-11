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
            aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${ecrRepo.split('/')[0]}
            docker tag ${imageName}:${imageTag} ${ecrImage}
            docker push ${ecrImage}
        """
    }
}
