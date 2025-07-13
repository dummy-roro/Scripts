def call() {
    sh 'find . -name "*.js" -exec node --check {} +'
}
