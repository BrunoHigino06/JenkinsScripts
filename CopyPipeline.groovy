pipeline {
    agent any

    environment{
        def find = ''
        def file = ''
        def hash = ''
        def hashfile = ''
        def deployhash = ''
    }

    stages {
        stage('Check File') {
            steps {

                script{

                    find = powershell (returnStdout:true, script: "{Test-Path -Path \\\\192.168.200.131\\ftp\\*}").trim()

                    if(find == "True"){
                        echo 'File not find on the folder'
                        
                    }
                    else{

                        currentBuild.result = 'FAILURE'
                        echo 'File not find on the folder'

                    }
                }
            }
        }
        stage('Check Hash'){
            when {
                expression{find == 'True'}
            }

            powershell (returnStdout:true, script: "Move-Item '\\\\192.168.200.131\\ftp\\*' -Destination '\\\\192.168.200.132\\inbox\\'")
            hashfile = powershell (returnStdout:true, script: "{Get-ChildItem -Path \\\\192.168.200.132\\inbox\\ -Filter *.txt}").trim()
            hash = powershell (returnStdout:true, script: "{Get-Content \\\\192.168.200.132\\inbox\\${hashfile} }").trim()
            deployhash = powershell (returnStdout:true, script: "{Get-FileHash \\\\192.168.200.132\\inbox\\${file} }").trim()

        }
    }
}