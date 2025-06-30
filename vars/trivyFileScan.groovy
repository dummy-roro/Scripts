def call(String path = '.', String output = 'trivy-fs-scan-report.html') {
    echo "🔍 Running Trivy filesystem scan on path: ${path}"
    
    sh """
        trivy fs --format html -o ${output} ${path}
    """
    
    echo "📄 Trivy FS scan report generated: ${output}"
}
