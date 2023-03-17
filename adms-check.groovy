/*
Before running this pipeline, please configure the environment variables below on Jenkins (Manage Jenkins >> Configure System >> Global Properties >> Flag Environment Variables). The values are examples.
emailRecipients = p.gesualdo.junior@accenture.com
*/

// Easy update variables
cumulativeUpdateLabel = "CU"
dllLabel = "dll"
hotFixLabel = "HF"
patchLabel = "PA"
sharedServerIp = "192.168.200.130"
outboxFolderPath = "C:\\Jenkins_folders\\shared_server_folders\\outbox"
sleepTimeSeconds = 5

// Other variables organized by name
calculatedHashValue = ""
deployableFileExtension = ""
deployableFileName = ""
counter = 0
folderNotEmpty = ""
fileToCheckName = ""
testConnection = ""
verifiedHashFileName = ""
verifiedHashValue = ""

// Function to test the connection with a server
def connectionSucessful(server) {

    // To return true if there is a successful connection with the server
    testConnection = powershell (script: "Test-Connection ${server} -Quiet",returnStdout: true).trim()

    // If the connection with the server is successfull
    if (testConnection == "True") return true
    else {
        echo "Failed to connect with the server ${server}"
        return false
    }
}

// Function to test if there is any file in a folder of a server
def folderNotEmpty(folderPath) {

    // To return true if there is any file
    folderNotEmpty = powershell (script: "Test-Path -Path ${folderPath}\\*", returnStdout: true).trim()

    // If any file was found
    if (folderNotEmpty == "True") return true
    else {
        echo "The folder ${folderPath} is empty or not accessible"
        return false
    }
}

// Function to get the data from the deployable file
def getDeployableFileData() {

    // To get the name of the deployable file
    deployableFileName = powershell (script: "(Get-ChildItem -Path \"${outboxFolderPath}\" -recurse -exclude *.txt).Name", returnStdout: true).trim()

    // To check if there is a deployable file
    if (deployableFileName != ""){
                            
        // To get the extension of the deployable file
        deployableFileExtension = powershell (script: "(Get-ChildItem -Path \"${outboxFolderPath}\" -recurse -exclude *.txt).Extension", returnStdout: true).trim()

        return true
    }
    else {
        echo "test"
        return false
    }
}

// Function to send an email to responsible team reporting a failure

// Function to check if the deployable file is actually deployable
def deployableFileChecked() {

    // To get the deployable file data such as file name
    getDeployableFileData()

    // To reset the counter
    counter = 0

    // To check if the deployable file has characteristics of more than one deploy possibility
    if(deployableFileName.contains(hotFixLabel)) counter++
    if(deployableFileName.contains(cumulativeUpdateLabel)) counter++
    if(deployableFileName.contains(patchLabel)) counter++
    if(deployableFileExtension.contains(dllLabel)) counter++
    
    // If the deployable file allows only one type of deployment (normal situation)
    if (counter == 1) return true

    // If the deployable file allows more than one type of deployment
    else if (counter > 1) {
        echo "test"
        return false
    }
    
    // If the deployable files does not allow any type of deployment
    else if (counter == 0) {
        echo "test"
        return false
    }
}

// Function to check the hash of a file
def hashChecked(fileToCheckFolderPath, verifiedHashFolderPath){

    // To get the name of the file to check
    fileToCheckName = powershell (script: "(Get-ChildItem -Path \"${fileToCheckFolderPath}\" -recurse -exclude *.txt).Name", returnStdout: true).trim()
    
    // To get the name of the verified hash file
    verifiedHashFileName = powershell (script: "(Get-ChildItem -Path \"${verifiedHashFolderPath}\" -recurse -Filter *.txt).Name", returnStdout: true).trim()

    // To check if there is a verified hash file
    if (verifiedHashFileName != "") {

        // To get the hash that is inside the .txt file
        verifiedHashValue = powershell (script: "type ${verifiedHashFolderPath}\\${verifiedHashFileName}", returnStdout: true).trim()

        // To check if the hash file has a hash value
        if (verifiedHashValue != ""){

            // To calculate the hash of the deployable file
            calculatedHashValue = powershell (script: "(Get-FileHash ${fileToCheckFolderPath}\\${fileToCheckName}).Hash", returnStdout: true).trim()

            // To check if the calculated hash is equals to verified hash
            if(calculatedHashValue == verifiedHashValue) return true
            else 
                echo "test"
        }
        else 
            echo "test"
    }
    else 
        echo "test"

    return false
}

// To check if the environment variable emailRecipients is valid
def emailRecipientsChecked() {
    
    environmentVariableName = "emailRecipients"

    // To reset the variable that tries again if the environment variable is missing
    tryAgain = "1"

    // To start the loop of checking the environment variable
    while (tryAgain == "1"){
            
        // If the environment variable is different from null, return true
        if (env.emailRecipients != null) return true
        else {                                        
            // Ask the user to try again or end the pipeline
            tryAgain = input message: "The environment variable ${environmentVariableName} is missing on Jenkins configuration.\nThis configuration is necessary to proceed with this pipeline.\nAs detailed at Deployment Automation documentation, please access Jenkins configuration through Manage Jenkins >> Configure System >> Environment Variables and configure the environment variable.\nThen, please return here and type 1 to try again or 0 to end the pipeline, than click Proceed.", parameters: [string(defaultValue: '', description: '', name: '')]
        }
    }
}

pipeline {
    agent any

    stages {
        stage('Check') {
            steps {
                script {
                    
                    // To repeat the pipeline
                    while(true) {

                        // If the connection with the Shared Server is successfull
                        // and the emailRecipients environment variable is checked
                        // and there is a file in the Shared Server
                        // and the deployable file is sucessfully checked
                        // and the hash is sucessfully checked
                        if (connectionSucessful(sharedServerIp) &&
                            emailRecipientsChecked() &&
                            folderNotEmpty(outboxFolderPath) && 
                            deployableFileChecked() && 
                            hashChecked(outboxFolderPath,outboxFolderPath)) {
                                        
                            // To capture any error, so the Check pipeline doesn't end if the Copy pipeline fails
                            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                                
                                // Call the "Copy" pipeline
                                build job: 'adms-copy'
                            }
                        }

                        // To wait before repeating the pipeline
                        sleep(time:sleepTimeSeconds,unit:"SECONDS")
                    }
                }
            }
        }
    }
}