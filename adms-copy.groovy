/*
Before running this pipeline, please configure the environment variables below on Jenkins (Manage Jenkins >> Configure System >> Global Properties >> Flag Environment Variables). The values are examples.
emailRecipients = p.gesualdo.junior@accenture.com
*/

// Easy update variables
deploymentServerIp = "172.31.11.171"
sharedServerIp = "172.31.10.34"
cumulativeUpdateLabel = "CU"
dllLabel = "dll"
hotFixLabel = "HF"
patchLabel = "PA"
triesAllowed = 5
sleepTimeSeconds = 60
inboxFolderPath = "\\\\${deploymentServerIp}\\adms\\inbox"
outboxFolderPath = "\\\\${sharedServerIp}\\adms\\outbox"

// Other variables organized by name
calculatedHashValue = 'x'
commandSucessful = true
stageSucessful = false
deployableFileExtension = ''
deployableFileName = ''
fileFound = ''
verifiedHashFileName = ''
verifiedHashValue = 'y'

// Function to call the environment pipeline passing the type of deployment as a parameter
def callEnvironmentPipeline(deployParam) {
    build job: 'adms-environment', parameters: [[$class: 'StringParameterValue', name: 'deployParam', value: deployParam]]
}

// Function to check the connection with a server. It fails the pipeline if there is there is no sucessful connection.
def checkConnection(server) {
    testConnection = powershell (script: "Test-Connection ${server} -Quiet",returnStdout: true).trim()
    if (testConnection == "False") failPipeline("Failed to connect with the server ${server}")
}

// To check if the environment variable emailRecipients is valid
def checkEnvironmentVariable_emailRecipients() {
    
    environmentVariableName = "emailRecipients"

    // To reset the variable that tries again if the environment variable is missing
    tryAgain = "1"

    // To start the loop of checking the environment variable
    while (tryAgain == "1"){
            
        // If the environment variable is null, ask the user to fix it and try again or end the pipeline
        if (env.emailRecipients == null){                                        
            
            // Ask the user to try again or end the pipeline
            tryAgain = input message: "The environment variable ${environmentVariableName} is missing on Jenkins configuration.\nThis configuration is necessary to proceed with this pipeline.\nAs detailed at Deployment Automation documentation, please access Jenkins configuration through Manage Jenkins >> Configure System >> Environment Variables and configure the environment variable.\nThen, please return here and type 1 to try again or 0 to end the pipeline, than click Proceed.", parameters: [string(defaultValue: '', description: '', name: '')]

            // If the user enters "0", end the pipeline as failed with error message
            if (tryAgain == 0) error("The environment variable ${environmentVariableName} is missing on Jenkins configuration.")
        }
        else {
            // Exit the loop
            break
        }
    }
}

// Functions to verify the hash of the deployable file
def checkHash() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&checkHash_Task
    check = this.&checkHash_Check
    ifFailure = this.&checkHash_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def checkHash_Task(){
    
    // To get the hash that is inside the .txt file
    verifiedHashValue = powershell (script: "type ${inboxFolderPath}\\${verifiedHashFileName} -ErrorAction Stop", returnStdout: true).trim()

    // To calculate the hash of the deployable file to later verify that the file is not corrupted
    calculatedHashValue = powershell (script: "(Get-FileHash ${inboxFolderPath}\\${deployableFileName} -ErrorAction Stop).Hash", returnStdout: true).trim()
}
def checkHash_Check(){
    // To check if the calculated hash is equals to verified hash
    if(calculatedHashValue == verifiedHashValue) return true
    else return false
}
def checkHash_IfFailure(){

    // To remove the deployable file from Deployment Server
    powershell "Remove-Item ${inboxFolderPath}\\${deployableFileName} -Recurse -Force"

    // To remove the hash file from Deployment Server
    powershell "Remove-Item ${inboxFolderPath}\\${verifiedHashFileName} -Recurse -Force"
    
    // To end the pipeline as failed, sending an email with a message and registering in Jenkins console the same message
    failPipeline("Failed to check the hash of the deployable file.\nThe hash of the deployable file ${inboxFolderPath}\\${deployableFileName} and the hash inside the hash file ${inboxFolderPath}\\${verifiedHashFileName} did not match.\nThe system tried for ${triesAllowed} times with interval of ${sleepTimeSeconds} seconds between tries.")
}

// Functions to copy the deployable file from Shared Server to Deployment Server
def copyDeployableFile() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyDeployableFile_Task
    check = this.&copyDeployableFile_Check
    ifFailure = this.&copyDeployableFile_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyDeployableFile_Task() {
    // To copy the deployable file from Outbox folder of Shared Server to Inbox folder of Deployment Server
    powershell "Copy-Item -Path ${outboxFolderPath}\\${deployableFileName} -Destination ${inboxFolderPath}\\${deployableFileName}"
}
def copyDeployableFile_Check() {
    // To check if the deployable file is in the inbox folder of the Deployment server. If yes, return true and if no, return false
    fileFound = powershell (script: "Test-Path -Path ${inboxFolderPath}\\${deployableFileName}", returnStdout: true).trim()
    if (fileFound == "True") return true
    else return false
}
def copyDeployableFile_IfFailure() {
    // To end the pipeline as failed, sending an email with a message and registering in Jenkins console the same message
    failPipeline("Failed to copy the deployable file ${deployableFileName} from Shared Server ${outboxFolderPath} to Deployment Server ${inboxFolderPath}\nThe system tried for ${triesAllowed} times with interval of ${sleepTimeSeconds} seconds between tries.")
}

// Functions to copy the hash file from Shared Server to Deployment Server
def copyHashFile() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyHashFile_Task
    check = this.&copyHashFile_Check
    ifFailure = this.&copyHashFile_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyHashFile_Task(){
    // To copy the hash file from Shared Server to Deployment Server
    powershell "Copy-Item -Path ${outboxFolderPath}\\${verifiedHashFileName} -Destination ${inboxFolderPath}\\${verifiedHashFileName}"
}
def copyHashFile_Check(){
    fileFound = powershell (script: "Test-Path -Path ${inboxFolderPath}\\${verifiedHashFileName}", returnStdout: true).trim()
    if (fileFound == "True") return true
    else return false   
}
def copyHashFile_IfFailure(){
    
    // Remove the deployable file from the Deployment Server to let the folder empty for the next deployment
    powershell "Remove-Item ${inboxFolderPath}\\${deployableFileName} -Recurse -Force"

    // To end the pipeline as failed, sending an email with a message and registering in Jenkins console the same message
    failPipeline("Failed to copy the hash file ${verifiedHashFileName} from Shared Server ${outboxFolderPath} to Deployment Server ${inboxFolderPath}\nThe system tried for ${triesAllowed} times with interval of ${sleepTimeSeconds} seconds between tries.")
}

// Function to end the pipeline as failed, sending an email with a message received as parameter and registering in Jenkins console the same message
def failPipeline(message) {
    sendFailureEmail(message)
    error(message)
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
        // To end the pipeline as failed, sending an email with a message received as parameter and registering in Jenkins console the same message
        failPipeline("Failed to find the deployable file in the Shared Server ${outboxFolderPath}")
    }
}

// Function to get information about the hash file
def getHashFileData() {

    // To get the name of the verified hash file
    verifiedHashFileName = powershell (script: "(Get-ChildItem -Path \"${outboxFolderPath}\" -recurse -Filter *.txt).Name", returnStdout: true).trim()

    // To end the pipeline as failed if the hash file is not found
    if (verifiedHashFileName == ""){
        
        // To end the pipeline as failed, sending an email with a message and registering in Jenkins console the same message
        failPipeline("Failed to find the hash file in the Shared Server ${outboxFolderPath}")
    }
}

// Functions to remove the deployable file from Shared Server
def removeDeployableFile() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&removeDeployableFile_Task
    check = this.&removeDeployableFile_Check
    ifFailure = this.&removeDeployableFile_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def removeDeployableFile_Task() {
    // To remove the deployable file from Shared Server
    powershell "Remove-Item ${outboxFolderPath}\\${deployableFileName} -Recurse -Force"
}
def removeDeployableFile_Check() {
    // To check again if the deployable file was removed from Shared Server
    fileFound = powershell (script: "Test-Path -Path ${outboxFolderPath}\\${deployableFileName}", returnStdout: true).trim()

    // If the file is not found, the check is successful, so this function returns true
    if (fileFound == "False") return true
    else return false 
}
def removeDeployableFile_IfFailure() {
    // To send an alert by email with a message and registering in Jenkins console the same message
    sendAlert("Failed to remove the deployable file ${deployableFileName} from the Shared Server ${inboxFolderPath}\nThe system tried for ${triesAllowed} times with interval of ${sleepTimeSeconds} seconds between tries.\nAction required: Please remove it manually")
}

// Functions to remove the hash file from Shared Server
def removeHashFile() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&removeHashFile_Task
    check = this.&removeHashFile_Check
    ifFailure = this.&removeHashFile_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def removeHashFile_Task() {
    // To remove the hash file from Shared Server
    powershell "Remove-Item ${outboxFolderPath}\\${verifiedHashFileName} -Recurse -Force"
}
def removeHashFile_Check() {
    // To check again if the hash file was removed from Shared Server
    fileFound = powershell (script: "Test-Path -Path ${outboxFolderPath}\\${verifiedHashFileName}", returnStdout: true).trim()

    // If the file is not found, the check is successful, so this function returns true
    if (fileFound == "False") return true
    else return false
}
def removeHashFile_IfFailure() {
    // To send an alert by email with a message and registering in Jenkins console the same message
    sendAlert("Failed to remove the hash file ${verifiedHashFileName} from the Shared Server ${inboxFolderPath}\nThe system tried for ${triesAllowed} times with interval of ${sleepTimeSeconds} seconds between tries.\nAction required: Please remove it manually")
}

// Function to send an alert by email with a message received as parameter and registering in Jenkins console the same message
def sendAlert(message) {
    sendFailureEmail(message)
    echo(message)
}

// Function to send an email to responsible team reporting a failure
def sendFailureEmail(message) {
    mail to: env.emailRecipients,
    subject: "Failure on pipeline ${JOB_NAME}, build ${BUILD_NUMBER}",
    body: "Failure message: ${message}\n\nPipeline: ${JOB_NAME}\n\nBuild: ${BUILD_NUMBER}\n\nLink: ${JOB_URL}"
}

// Function to try to execute a task many times, check if the task was sucessfull or execute something in case of failure
def tryManyTimes(task,check,ifFailure) {

    // To reset the counter of tries to execute the task
    int commandTries = 0

    // To create a loop to try to execute the task many times
    while (true) {
        
        // To increase the counter of tries
        commandTries++

        // To reset the command controller as if the command is sucessful, because if it is not sucessful it will entry the catch and will set the controller as unsucessful
        commandSucessful = true
        
        // Try to execute the task that was received as a parameter
        try { 
            task()
        }
        // If there is an error during the execution of the task above
        catch(Exception e1) {

            // Set the command controller as unsucessful
            commandSucessful = false
            
            // If the system tried to execute the command less than the tries allowed times, wait before trying again
            if (commandTries < triesAllowed) sleep(time:sleepTimeSeconds,unit:"SECONDS")
            else {
                
                // In case of failure, execute a function that was received as a parameter
                ifFailure()

                // Exit the command loop
                break
            }
        }

        // If the execution of the task received as a parameter did not return any error
        if (commandSucessful == true) {

            // To execute a function to check if the command above was executed sucessfully, because sometimes the command does not return any error but is not executed correctly
            if (check() == true) break
            else{
                
                // Set the command controller as unsucessful
                commandSucessful = false

                // If the system tried to execute the command less than the tries allowed, wait before trying again
                if (commandTries < triesAllowed) sleep(time:sleepTimeSeconds,unit:"SECONDS")
                else {
                    
                    // In case of failure, execute a function that was received as a parameter
                    ifFailure()
                    
                    // Exit the command loop
                    break
                }
            }
        }
    }
}

pipeline {
    agent any
    stages {
        stage('To check connection') {
            steps {
                script {
                    // To check connection with Shared Server and Deployment Server
                    checkConnection(sharedServerIp)
                    checkConnection(deploymentServerIp)
                }
            }
        }
        stage('To check environment variables') {
            steps {
                script {
                    // To check the emailRecipients environment variable
                    checkEnvironmentVariable_emailRecipients()
                }
            }
        }
        stage('To copy files') {
            steps {
                script {
                    // To get information about the deployable file
                    getDeployableFileData()
                    // To get information about the hash file
                    getHashFileData()
                    // Copy the deployable file from Shared Server to Deployment Server
                    copyDeployableFile()
                    // Copy the hash file from Shared Server to Deployment Server
                    copyHashFile()
                    // Verify hash of the deployable file
                    checkHash()
                    // Remove the deployable file from Shared Server
                    removeDeployableFile()
                    // Remove the hash file from Shared Server
                    removeHashFile()
                }
            }
        }
        stage('To check deployment type and deploy') {
            steps {
                script {
                    // To check which type of deploy based on the deployable file name or type, and call the respective pipeline
                    if(deployableFileName.contains(hotFixLabel)) callEnvironmentPipeline("HF")
                    else if(deployableFileName.contains(cumulativeUpdateLabel)) callEnvironmentPipeline("CU")
                    else if(deployableFileName.contains(patchLabel)) callEnvironmentPipeline("PA")
                    else if(deployableFileExtension.contains(dllLabel)) callEnvironmentPipeline("DLL")
                }
            }
        }
    }
}