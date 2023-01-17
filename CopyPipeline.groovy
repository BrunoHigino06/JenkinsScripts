pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('Copy File') {
            steps {

                script{

                    try{
                    file = powershell (returnStdout:true, script: "Get-ChildItem -Path \\\\192.168.200.131\\ftp\\ | select Name").trim()
                    powershell (returnStdout:true, script: "Copy-Item '\\\\192.168.200.131\\ftp\\${file}' -Destination '\\\\192.168.200.132\\ftp\\inbox\\'")                        
                    
                    }catch(Exception e){

                    }



                }
            }
        }
    }
}