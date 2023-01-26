pipeline {
    agent any
   
    stages {
        stage("Start the Deployment") {
            steps {
                env.DevServers.tokenize(",").each { server ->
                    stage(server){
                        echo "Server is $server in parallel"
                    }
                }
            }
        }
    }
}