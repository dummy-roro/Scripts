def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def repoUrl = config.repoUrl ?: 'https://github.com/LondheShubham153/tws-e-commerce-app.git'
    def branch = env.GIT_BRANCH ?: 'main'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Optionally clone repo fresh to a directory 'gitops'
        sh """
            rm -rf gitops
            git clone --branch ${branch} https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/LondheShubham153/tws-e-commerce-app.git gitops
        """

        dir('gitops') {
            sh """
                git config user.name "${gitUserName}"
                git config user.email "${gitUserEmail}"
                
                sed -i "s|image: trainwithshubham/easyshop-app:.*|image: dummyroro/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
                
                if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                    sed -i "s|image: trainwithshubham/easyshop-migration:.*|image: dummyroro/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
                fi
                
                # if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                #    sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
                fi
                
                if git diff --quiet; then
                    echo "No changes to commit"
                else
                    git add ${manifestsPath}/*.yaml
                    git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                    git push origin HEAD:${branch}
                fi
            """
        }
    }
}
