pipeline {
    agent any

    stages {
        stage('Smoke Tests') {
            steps {
                script {
                    echo "Hello smoke tests"
                }
                
            }
        }
    }
}