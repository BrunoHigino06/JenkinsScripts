pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('Copy File') {
            steps {

                script{

                    find = powershell (returnStdout:true, script: "{Test-Path -Path \\\\192.168.200.131\\ftp\\*}") 

                    if(find == "True"){

                        file = powershell (returnStdout:true, script: "Get-ChildItem -Path \\\\192.168.200.131\\ftp\\ | select Name").trim()
                        powershell (returnStdout:true, script: "Copy-Item '\\\\192.168.200.131\\ftp\\${file}' -Destination '\\\\192.168.200.132\\inbox\\'")    

                    }
                    else{

                        currentBuild.result = 'FAILURE'
                        echo 'Error to copy the file to the deployment server'

                    }
                }
            }
        }
    }
}