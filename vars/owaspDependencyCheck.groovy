def call(List<String> dirs = []) {
    if (dirs.isEmpty()) {
        echo "Running OWASP Dependency Check in root directory"
        runDependencyCheck('.', 'root')
    } else {
        dirs.each { dir ->
            echo "Running OWASP Dependency Check in ${dir}"
            runDependencyCheck("./${dir}", dir)
        }
    }
}

private void runDependencyCheck(String scanPath, String reportName) {
    dependencyCheck additionalArguments: """
        --scan ${scanPath}
        --out ./reports/${reportName}
        --format ALL
        --prettyPrint
    """, odcInstallation: 'OWASP-DepCheck-10'

    dependencyCheckPublisher pattern: "reports/${reportName}/dependency-check-report.xml", failedTotalCritical: 1
}
