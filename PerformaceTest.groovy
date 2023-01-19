pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('Performace Test') {
            steps {

                script{
                    runLoadRunnerScript 'c:\\'
                }
            }
        }
    }
}