def call(String dirPath) {
    dir(dirPath) {
        sh 'find . -name "*.js" -exec node --check {} +'
    }
}
