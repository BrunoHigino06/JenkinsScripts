def deploymentStages = [:]

pipeline {
    agent any
   
    stages {
        stage("Start the Deployment") {
            steps {
                script {
                    env.DevServers.tokenize(",").each { server ->
                        deploymentStages["${server.key}"] = {
                            stage(server){
                                echo "Server is $server in parallel"
                            }
                        }   
                    }
                    parallel deploymentStages
                }
            }
        }
    }
}


