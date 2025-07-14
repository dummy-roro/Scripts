def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def repoUrl = config.repoUrl ?: 'https://github.com/<your-gitops-repo>/name.git'
    def branch = env.GIT_BRANCH ?: 'dev'

    echo "Updating Kubernetes deployment manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Clone repo fresh to directory 'gitops'
        sh """
            rm -rf gitops
            git clone --branch ${branch} https://${GIT_USERNAME}:${GIT_PASSWORD}@${repoUrl.replace('https://','')} gitops
        """

        dir('gitops') {
            sh """
                git config user.name "${gitUserName}"
                git config user.email "${gitUserEmail}"
            """

            // Run updates in parallel shell background tasks
            // We'll update frontend and backend deployment manifests in parallel
            sh """
                (
                    sed -i "s|image: trainwithshubham/easyshop-frontend:.*|image: dummyroro/easyshop-frontend:${imageTag}|g" ${manifestsPath}/frontend-deployment.yaml
                )
                wait
            """

            // Commit and push if changes found
            sh """
                if git diff --quiet; then
                    echo "No changes to commit"
                else
                    git add ${manifestsPath}/frontend-deployment.yaml ${manifestsPath}/backend-deployment.yaml
                    git commit -m "Update frontend and backend image tags to ${imageTag} [ci skip]"
                    git push origin HEAD:${branch}
                fi
            """
        }
    }
}
