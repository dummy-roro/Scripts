def call(List<String> dirs = []) {
    dirs.each { dir ->
        echo "Running npm audit in ${dir}"
        dir(dir) {
            sh 'npm install --legacy-peer-deps' // Optional
            sh 'npm audit --audit-level=critical'
        }
    }
}
