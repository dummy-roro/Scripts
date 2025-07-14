def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def repoUrl = config.repoUrl ?: 'https://github.com/<your-gitops-repo>/name.git'
    def branch = env.GIT_BRANCH ?: 'main'
    def ecrRepo = config.ecrRepo ?: error("ECR repo prefix is required") // e.g. 123456789012.dkr.ecr.us-east-1.amazonaws.com

    echo "Updating dashboard-app image to ${ecrRepo}/dashboard-app:${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            set -e
            rm -rf gitops
            git clone --branch ${branch} https://${GIT_USERNAME}:${GIT_PASSWORD}@${repoUrl.replace('https://','')} gitops
        """

        dir('gitops') {
            sh """
                set -e
                git config user.name "${gitUserName}"
                git config user.email "${gitUserEmail}"
            """

            sh """
                set -e
                sed -i "s|image: .*dashboard-app:.*|image: ${ecrRepo}/dashboard-app:${imageTag}|g" ${manifestsPath}/dashboard-deployment.yaml
            """

            sh """
                set -e
                if git diff --quiet; then
                    echo "No changes to commit"
                else
                    git add ${manifestsPath}/dashboard-deployment.yaml
                    git commit -m "Update dashboard-app image tag to ${imageTag} [ci skip]"
                    git push origin HEAD:${branch}
                fi
            """
        }
    }
}
