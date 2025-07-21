def call(Map config = [:]) {
    def imageName = config.imageName ?: error("imageName is required")
    def imageTag = config.imageTag ?: "latest"
    def outputDir = config.outputDir ?: "."
    def failOnCritical = config.get('failOnCritical', true)

    echo "[*] Running Trivy scan + HTML report for image: ${imageName}:${imageTag}"

    // Ensure output directory exists
    sh "mkdir -p \"${outputDir}\""

    // Scan LOW, MEDIUM, HIGH
    sh """
        trivy image ${imageName}:${imageTag} \\
            --severity LOW,MEDIUM,HIGH \\
            --exit-code 0 \\
            --quiet \\
            --format json -o "${outputDir}/trivy-image-medium-results.json"
    """

    // Generate HTML report for LOW, MEDIUM, HIGH
    sh """
        trivy convert \\
            --format template \\
            --template "@/usr/local/share/trivy/templates/html.tpl" \\
            --output "${outputDir}/trivy-image-medium-results.html" \\
            "${outputDir}/trivy-image-medium-results.json"
    """

    // Initialize flag to track critical scan failure
    def criticalScanFailed = false

    // Run critical scan with failure control
    try {
        sh """
            trivy image ${imageName}:${imageTag} \\
                --severity CRITICAL \\
                --exit-code ${failOnCritical ? 1 : 0} \\
                --quiet \\
                --format json -o "${outputDir}/trivy-image-critical-results.json"
        """
    } catch (Exception e) {
        criticalScanFailed = true
        echo "Critical vulnerabilities detected in image scan."
        if (!failOnCritical) {
            echo "FailOnCritical is false, continuing pipeline despite critical issues."
        }
    }

    // Always generate HTML report for critical vulnerabilities
    sh """
        trivy convert \\
            --format template \\
            --template "@/usr/local/share/trivy/templates/html.tpl" \\
            --output "${outputDir}/trivy-image-critical-results.html" \\
            "${outputDir}/trivy-image-critical-results.json"
    """

    // Fail pipeline if critical vulnerabilities found
    if (criticalScanFailed && failOnCritical) {
        error("Pipeline failed due to critical vulnerabilities found in ${imageName}:${imageTag}")
    }

    echo "[*] Trivy scan and report generation complete for ${imageName}:${imageTag}."
}
