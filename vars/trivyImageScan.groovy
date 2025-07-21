def call(Map config = [:]) {
    def imageName    = config.imageName    ?: error('imageName is required')
    def imageTag     = config.imageTag     ?: 'latest'
    def outputDir    = config.outputDir    ?: '.'
    def failOnCritical = config.get('failOnCritical', true)
    def htmlTemplate = config.htmlTemplate ?: '/usr/local/share/trivy/templates/html.tpl'

    sh "mkdir -p '${outputDir}'"

    // 1) SARIF for Jenkins warnings-ng plugin / recordIssues
    sh """
        trivy image ${imageName}:${imageTag} \
            --format sarif \
            --output '${outputDir}/trivy.sarif' \
            --severity UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL
    """

    // 2) JSON + optional HTML for LOW,MEDIUM,HIGH
    sh """
        trivy image ${imageName}:${imageTag} \
            --severity LOW,MEDIUM,HIGH \
            --exit-code 0 \
            --format json \
            --output '${outputDir}/trivy-image-medium-results.json'
    """
    sh """
        trivy convert \
            --format template \
            --template '@${htmlTemplate}' \
            --output '${outputDir}/trivy-image-medium-results.html' \
            '${outputDir}/trivy-image-medium-results.json'
    """

    // 3) Critical scan with pipeline gate
    def criticalFail = false
    try {
        sh """
            trivy image ${imageName}:${imageTag} \
                --severity CRITICAL \
                --exit-code ${failOnCritical ? 1 : 0} \
                --format json \
                --output '${outputDir}/trivy-image-critical-results.json'
        """
    } catch (e) {
        criticalFail = true
        echo 'Critical vulnerabilities detected in image scan.'
    }

    sh """
        trivy convert \
            --format template \
            --template '@${htmlTemplate}' \
            --output '${outputDir}/trivy-image-critical-results.html' \
            '${outputDir}/trivy-image-critical-results.json'
    """

    if (criticalFail && failOnCritical) {
        error("Pipeline failed due to critical vulnerabilities in ${imageName}:${imageTag}")
    }

    echo "[✓] Trivy scan complete for ${imageName}:${imageTag}"
}
