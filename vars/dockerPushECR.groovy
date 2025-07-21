def call(Map config = [:]) {
    def imageName     = config.imageName   ?: error('imageName is required')
    def imageTag      = config.imageTag    ?: 'latest'
    def awsRegion     = config.awsRegion   ?: 'us-east-1'
    def ecrRepo       = config.ecrRepo     ?: error('ecrRepo is required')
    def awsCredsId    = config.credentials ?: 'aws-ecr-credentials'

    def ecrImage = "${ecrRepo}:${imageTag}"

    echo "📤 Pushing ${imageName}:${imageTag} → ${ecrImage}"

    withAWS(region: awsRegion, credentials: awsCredsId) {
        sh """
            set -e
            aws ecr get-login-password --region ${awsRegion} | \
              docker login --username AWS --password-stdin ${ecrRepo.split('/')[0]}

            docker tag ${imageName}:${imageTag} ${ecrImage}
            docker push ${ecrImage}
        """
    }
}
