def call(String imageName) {
    sh "trivy image --format table ${imageName}"
}
