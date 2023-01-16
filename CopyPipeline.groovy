pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('Copy') {
            steps {

                script{

                    powershell (returnStdout:true, script: "Invoke-Command -ComputerName 192.168.200.131 -ScriptBlock {Test-Path -Path C:\\FTP\\inbox\\*}").trim()

                }
            }
        }
    }
}