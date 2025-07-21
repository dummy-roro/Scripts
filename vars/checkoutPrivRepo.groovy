def call(String url, String branch, String credentialsId) {
    checkout([
        $class           : 'GitSCM',
        branches         : [[name: branch]],
        extensions       : [[$class: 'CleanCheckout']],
        userRemoteConfigs: [[
            url          : url,
            credentialsId: credentialsId
        ]]
    ])
}
