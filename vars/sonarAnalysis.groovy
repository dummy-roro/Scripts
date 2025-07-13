def call(String projectKey, String projectName, String projectVersion = '1.0') {
    withSonarQubeEnv('SonarServer') {
        sh """
            sonar-scanner \
                -Dsonar.projectKey=${projectKey} \
                -Dsonar.projectName=${projectName} \
                -Dsonar.projectVersion=${projectVersion} \
                -Dsonar.sources=. \
                -Dsonar.javascript.node.maxspace=4096
        """
    }
}
