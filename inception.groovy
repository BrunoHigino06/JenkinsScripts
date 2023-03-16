def commonFunction () {
    echo "Hello inception"
}

def commonFunctionAsVariable = this.&commonFunction

def callFunction(functionAsVariable) {
    functionAsVariable()
}

pipeline {
    agent any

    stages {
        stage('Inception') {
            steps {
                script {
                    callFunction(commonFunctionAsVariable)
                } 
            }
        }
    }
}