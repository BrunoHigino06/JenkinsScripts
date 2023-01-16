pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('CheckFile') {
            steps {

                script{
                    find = powershell (returnStdout:true, script: "Invoke-Command -ComputerName 192.168.200.131 -ScriptBlock {Test-Path -Path C:\\FTP\\inbox\\*}").trim()

                    echo find 

                    if(find == "True"){
                        echo "True"
                    }
                    else{
                        echo "False"
                    }
                }
            }
        }
    }
}