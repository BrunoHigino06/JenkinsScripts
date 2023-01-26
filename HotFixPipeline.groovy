pipeline {
    agent any
   
    stages {
        env.DevServers.tokenize(",").each { server ->
            stage("$server") {
                steps {
                    script {
                        echo "Server is $server"
                    }
                }
            }
        }
    }
}