def call(List<String> dirs = []) {
    dirs.each { dir ->
        echo "Running OWASP Dependency Check in ${dir}"
        dependencyCheck additionalArguments: """
            --scan ./${dir} 
            --out ./reports/${dir}
            --format ALL
            --prettyPrint
        """, odcInstallation: 'OWASP-DepCheck-10'

        dependencyCheckPublisher pattern: "reports/${dir}/dependency-check-report.xml", failedTotalCritical: 1
    }
}
