def call(String url, String branch, String credentialsId) {
    git url: url, branch: branch, credentialsId: credentialsId
}
