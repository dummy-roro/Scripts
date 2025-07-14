def call(Map config = [:]) {
    def imageName = config.imageName ?: error("Image name is required")
    def imageTag = config.imageTag ?: 'latest'
    def context = config.context ?: '.'
    def dockerfile = config.dockerfile ?: 'Dockerfile'

    echo "Building Docker image: ${imageName}:${imageTag} using Dockerfile '${dockerfile}' and context '${context}'"

    sh """
        docker build -t "${imageName}:${imageTag}" -t "${imageName}:latest" -f "${dockerfile}" "${context}"
    """
}
