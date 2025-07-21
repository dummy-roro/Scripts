def call(Map config = [:]) {
    def imageName = config.imageName ?: error('Image name is required')
    def imageTag  = config.imageTag  ?: 'latest'
    def context   = config.context   ?: '.'
    def dockerfile= config.dockerfile?: 'Dockerfile'

    echo "Building Docker image: ${imageName}:${imageTag} (Dockerfile: ${dockerfile}, context: ${context})"

    sh """
        docker build \
          --pull \
          --no-cache \
          -t "${imageName}:${imageTag}" \
          -t "${imageName}:latest" \
          -f "${dockerfile}" \
          "${context}"
    """
}
