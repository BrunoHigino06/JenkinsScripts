def tasks = [:]

pipeline {
    agent any
    
   
    stages {
        stage("Start the Deployment") {
            steps {
                script {
                    env.DevServers.tokenize(",").each { server ->
                        tasks[server] = {
                            stage(server){
                                echo "Server is $server in parallel"
                            }
                        }   
                    }
                    parallel tasks
                }
            }
        }
    }
}