def call(String path, String output) {
    sh "trivy fs --format table -o ${output} ${path}"
}
