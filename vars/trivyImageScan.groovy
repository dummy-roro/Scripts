def call(Map config = [:]) {
    def imageName = config.imageName ?: error("imageName is required")
    def imageTag = config.imageTag ?: "latest"
    def outputDir = config.outputDir ?: "."
    def failOnCritical = config.get('failOnCritical', true)

    echo "[*] Running full Trivy scan + HTML report for: ${imageName}:${imageTag}"

    sh """
        mkdir -p "${outputDir}"

        trivy image ${imageName}:${imageTag} \\
            --severity LOW,MEDIUM,HIGH \\
            --exit-code 0 \\
            --quiet \\
            --format json -o "${outputDir}/trivy-image-MEDIUM-results.json"

        trivy image ${imageName}:${imageTag} \\
            --severity CRITICAL \\
            --exit-code ${failOnCritical ? 1 : 0} \\
            --quiet \\
            --format json -o "${outputDir}/trivy-image-CRITICAL-results.json"

        trivy convert \\
            --format template \\
            --template "@/usr/local/share/trivy/templates/html.tpl" \\
            --output "${outputDir}/trivy-image-MEDIUM-results.html" \\
            "${outputDir}/trivy-image-MEDIUM-results.json"

        trivy convert \\
            --format template \\
            --template "@/usr/local/share/trivy/templates/html.tpl" \\
            --output "${outputDir}/trivy-image-CRITICAL-results.html" \\
            "${outputDir}/trivy-image-CRITICAL-results.json"
    """

    echo "[*] Trivy full scan complete. HTML reports generated in ${outputDir}"
}
