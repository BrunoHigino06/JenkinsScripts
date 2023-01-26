pipeline {
    agent any
   
    stages {
        
        stage("Start the pipeline") {
            steps {
                script {
                    env.DevServers.tokenize(",").each { server ->
                        for(int i=0; i < server.size(); i++) {
                            stage(${server}){
                                echo "Server is $server"
                            }
                        }
                        echo "Server is $server"
                    }
                }
            }
        }
    }
}