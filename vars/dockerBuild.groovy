def call(Map config = [:]) {
    def imageName = config.imageName ?: error('Image name is required')
    def imageTag  = config.imageTag  ?: error('imageTag is required')  // fail fast
    def context   = config.context   ?: '.'
    def dockerfile= config.dockerfile?: 'Dockerfile'

    echo "Building Docker image: ${imageName}:${imageTag}"

    sh """
        docker build \
          --pull \
          --no-cache \
          -t "${imageName}:${imageTag}" \
          -f "${dockerfile}" \
          "${context}"
    """
}
