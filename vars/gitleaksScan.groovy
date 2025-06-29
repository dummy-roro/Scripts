def call(List<String> dirs) {
    dirs.each { dir ->
        sh "gitleaks detect --source ./${dir} --exit-code 1"
    }
}
