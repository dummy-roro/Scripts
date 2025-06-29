def call(String projectName, String projectKey) {
    withSonarQubeEnv('sonar') {
        sh """${tool('sonar-scanner')}/bin/sonar-scanner \
            -Dsonar.projectName=${projectName} \
            -Dsonar.projectKey=${projectKey}"""
    }
}
