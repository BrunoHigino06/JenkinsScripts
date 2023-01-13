pipeline {
    agent any

    environment{
        def find = ''
    }

    stages {
        stage('CheckFile') {
            steps {
                echo 'Checking the directory Inbox for new files'

                ${find} = powershell "Test-Path -Path \\192.168.200.140\\C:\\Users\\Bruno\\Downloads\\test\\*"
                if(${find} = True){
                    powershell ""
                }
                else {
                    echo 'File not found'
                }
            }
        }
    }
}