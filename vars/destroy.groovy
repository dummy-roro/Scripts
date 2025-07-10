def call(Map config = [:]) {
    withAWS(credentials: config.awsCreds, region: config.awsRegion) {
        sh "terraform -chdir=${config.terraformDir} destroy -var-file=${config.env}.tfvars -auto-approve"
    }
}
