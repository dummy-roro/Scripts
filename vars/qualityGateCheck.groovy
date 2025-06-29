def call(String credentialsId) {
    timeout(time: 1, unit: 'HOURS') {
        waitForQualityGate abortPipeline: false, credentialsId: credentialsId // set to abortPipeline: true for real testing
    }
}
