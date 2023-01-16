pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('Copy') {
            steps {

                script{

                    file = powershell (returnStdout:true, script: "Invoke-Command -ComputerName 192.168.200.131 -ScriptBlock {Get-ChildItem -Path C:\\FTP\\inbox\\ | select Name}").trim()

                    powershell (returnStdout:true, script: "Invoke-Command -ComputerName 192.168.200.131 -ScriptBlock {...}")

                }
            }
        }
    }
}