pipeline {
    agent any
   
    stages {
        
        stage("Start the Deployment") {
            parallel {
                steps {
                    script {
                        env.DevServers.tokenize(",").each { server ->
                            stage(server){
                                echo "Server is $server in parallel"
                            }
                        }
                    }
                }
            }
        }
    }
}