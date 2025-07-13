def call() {
    sh 'npm install --legacy-peer-deps'
    sh 'npm audit --audit-level=critical'
}
