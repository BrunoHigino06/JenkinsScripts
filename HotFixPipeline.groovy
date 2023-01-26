pipeline {
    agent any
   
    stages {
        stage("Start the Deployment") {
            steps {
                parallel (
                    script {
                        env.DevServers.tokenize(",").each { server ->
                            stage(server){
                                echo "Server is $server in parallel"
                            }
                        }
                    }
                )
                
            }
        }
    }
}