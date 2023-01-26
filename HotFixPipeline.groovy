pipeline {
    agent any

    environment {
    }
    
    stages {
        stage("Application Sync") {
            steps {
                script {
                    env.DevServers.tokenize(",").each { server ->
                        echo "Server is $server"
                    }
                }
            }
        }
    }
}