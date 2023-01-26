pipeline {
    agent any
   
    stages {
        
        stage("Start the pipeline") {
            steps {
                script {
                    env.DevServers.tokenize(",").each { server ->
                        stage("${server}") {
                            steps {
                                script {
                                    echo "Server is $server"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}