def trivyScan(Map config = [:]) {
    def imageName = config.imageName ?: error("imageName is required")
    def imageTag = config.imageTag ?: "latest"

    echo "[*] Running Trivy scan for: ${imageName}:${imageTag}"

    sh """
        trivy image ${imageName}:${imageTag} \\
            --severity LOW,MEDIUM,HIGH \\
            --exit-code 0 \\
            --quiet \\
            --format json -o trivy-image-MEDIUM-results.json

        trivy image ${imageName}:${imageTag} \\
            --severity CRITICAL \\
            --exit-code 1 \\
            --quiet \\
            --format json -o trivy-image-CRITICAL-results.json
    """
}

def trivyConvertReports() {
    echo "[*] Generating HTML reports..."

    sh """
        trivy convert \\
            --format template --template "@/usr/local/share/trivy/templates/html.tpl" \\
            --output trivy-image-MEDIUM-results.html trivy-image-MEDIUM-results.json 

        trivy convert \\
            --format template --template "@/usr/local/share/trivy/templates/html.tpl" \\
            --output trivy-image-CRITICAL-results.html trivy-image-CRITICAL-results.json 
    """
}
