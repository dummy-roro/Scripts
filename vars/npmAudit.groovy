def call(List<String> dirs = []) {
    if (dirs.isEmpty()) {
        echo "Running npm audit in root"
        sh 'npm install --legacy-peer-deps'
        sh 'npm audit --audit-level=critical'
    } else {
        dirs.each { dir ->
            echo "Running npm audit in ${dir}"
            sh "npm --prefix ${dir} install --legacy-peer-deps"
            sh "npm --prefix ${dir} audit --audit-level=critical"
        }
    }
}
