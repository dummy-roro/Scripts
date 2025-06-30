def call(Map config = [:]) {
    def imageName = config.imageName ?: error("Docker image name is required")
    def imageTag = config.imageTag ?: 'latest'
    def outputFile = config.outputFile ?: "${imageName.replaceAll('/', '_')}-${imageTag}-trivy-image-report.html"

    echo "🔍 Scanning Docker image: ${imageName}:${imageTag} with Trivy"

    sh """
        trivy image --format html -o ${outputFile} ${imageName}:${imageTag}
    """

    echo "📄 Trivy scan report generated: ${outputFile}"
}
