pipeline {
    agent any
    environment{
        def tasks = ''
    }
   
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