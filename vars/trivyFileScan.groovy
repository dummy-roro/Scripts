def call(String path = '.', String output = 'trivy-fs-scan-report.html') {    
    sh """
        trivy fs --format html -o ${output} ${path}
    """
    echo "📄 Trivy FS scan report generated: ${output}"
}
