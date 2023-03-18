/* 
Before running this pipeline, please configure the environment variables below on Jenkins (Manage Jenkins >> Configure System >> Global Properties >> Flag Environment Variables). The values are examples.
hfCommands = ["SignOutUsers", "StopServices", "InstallHotFix", "StartServices"]
cuCommands = ["SignOutUsers", "StopServices", "InstallCumulativeUpdate", "StartServices"]
paCommands = ["SignOutUsers", "StopServices", "InstallPatch", "StartServices"]
dllCommands = ["SignOutUsers", "StopServices", "InstallDll", "StartServices"]
devServers = ["core1RT1", "core1RT2", "core1HIST1", "core1HIST2"]
emailRecipients = p.gesualdo.junior@accenture.com
*/

// Easy update variables
deploymentServerIp = "192.168.200.130"
cumulativeUpdateLabel = "CU"
dllLabel = "DLL"
hotFixLabel = "HF"
patchLabel = "PA"
environmentParam = "DEV"
deployedFolderPath = "C:\\Jenkins_folders\\jenkins_server_folders\\deployed"
failedFolderPath = "C:\\Jenkins_folders\\jenkins_server_folders\\failed"
inboxFolderPath = "C:\\Jenkins_folders\\jenkins_server_folders\\inbox"
partialDeployedFolderPath = "C:\\Jenkins_folders\\jenkins_server_folders\\partial"
sleepTimeSeconds = 1
triesAllowed = 5
failurePercentage = 70

// Other variables organized by name
calculatedHashValue = "x"
commandsArray = []
commandsList = ""
deployableFileName = ""
deploymentServerConnectionSuccessful = ""
def parallelDeployments
serversList = ""
serversArray = []
tryAgain = "1"
unsucessfulDeploymentArray = []
verifiedHashFileName = ""
verifiedHashValue = "y"

// Function to check the connection with a server. It fails the pipeline if there is there is no sucessful connection.
def checkConnection(server) {
    testConnection = powershell (script: "Test-Connection ${server} -Quiet",returnStdout: true).trim()
    if (testConnection == "False") failPipeline("Failed to connect with the server ${server}")
}

// Functions to check if the environment variables are valid
def checkEnvironmentVariable_cuCommands() {
    // To start the loop of checking the environment variable
    while (tryAgain == "1"){
        // If the environment variable is not null, 
        if (env.cuCommands != null){                                        
            // The commands that will be executed are the commands that are in the environment variable
            commandsList = env.cuCommands
            // To end the loop
            tryAgain = "0"
        }
        else {
            // Ask the user to fix it and try again or end the pipeline      
            sendEnvironmentVariableFail("cuCommands")
        }
    }
}
def checkEnvironmentVariable_dllCommands() {
    // To start the loop of checking the environment variable
    while (tryAgain == "1"){
        // If the environment variable is not null, 
        if (env.dllCommands != null){                                        
            // The commands that will be executed are the commands that are in the environment variable
            commandsList = env.dllCommands
            // To end the loop
            tryAgain = "0"
        }
        else {
            // Ask the user to fix it and try again or end the pipeline      
            sendEnvironmentVariableFail("dllCommands")
        }
    }
}
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
def checkEnvironmentVariable_hfCommands() {
    // To start the loop of checking the environment variable
    while (tryAgain == "1"){
        // If the environment variable is not null, 
        if (env.hfCommands != null){                                        
            // The commands that will be executed are the commands that are in the environment variable
            commandsList = env.hfCommands
            // To end the loop
            tryAgain = "0"
        }
        else {
            // Ask the user to fix it and try again or end the pipeline      
            sendEnvironmentVariableFail("hfCommands")
        }
    }
}
def checkEnvironmentVariable_paCommands() {
    // To start the loop of checking the environment variable
    while (tryAgain == "1"){
        // If the environment variable is not null, 
        if (env.paCommands != null){                                        
            // The commands that will be executed are the commands that are in the environment variable
            commandsList = env.paCommands
            // To end the loop
            tryAgain = "0"
        }
        else {
            // Ask the user to fix it and try again or end the pipeline      
            sendEnvironmentVariableFail("paCommands")
        }
    }
}

// To check if there is a list of commands for the type of deployment
def checkListOfCommands(){
    switch(params.deployParam) {
        case hotFixLabel:           checkEnvironmentVariable_hfCommands(); break;
        case cumulativeUpdateLabel: checkEnvironmentVariable_cuCommands(); break;
        case patchLabel:            checkEnvironmentVariable_paCommands(); break;
        case dllLabel:              checkEnvironmentVariable_dllCommands(); break;
        default: failPipeline("Failed to identify deployment type or list of commands")
    }
}

// Function to check a parameter. It fails the pipeline if the parameter is null.
def checkParameter(parameterName,parameterValue) {
    if (parameterValue == "") failPipeline("Failed to receive the following parameter: ${parameterName}")
}

// Functions to copy the deployable file from Inbox folder to Deployed folder in the Deployment Server
def copyDeployableFileToDeployed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyDeployableFileToDeployed_Task
    check = this.&copyDeployableFileToDeployed_Check
    ifFailure = this.&copyDeployableFileToDeployed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyDeployableFileToDeployed_Task() {
    // To copy the deployable file from Inbox folder to Deployed folder in Deployment Server
    powershell "Copy-Item -Path ${inboxFolderPath}\\${deployableFileName} -Destination ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}"
}
def copyDeployableFileToDeployed_Check() {

    // To check if the deployable file was copied
    fileFound = powershell (script: "Test-Path -Path ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}", returnStdout: true).trim()

    // If the file is found
    if (fileFound == "True") return true
    else return false
}
def copyDeployableFileToDeployed_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file could not be copied from Inbox folder to Deployed folder, please copy it manually. Name of the destination file: ${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}")
}

// Functions to copy the deployable file from Inbox folder to Failed folder in the Deployment Server
def copyDeployableFileToFailed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyDeployableFileToFailed_Task
    check = this.&copyDeployableFileToFailed_Check
    ifFailure = this.&copyDeployableFileToFailed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyDeployableFileToFailed_Task() {
    // To copy the deployable file from Inbox folder to Failed folder in Deployment Server
    powershell "Copy-Item -Path ${inboxFolderPath}\\${deployableFileName} -Destination ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}"
}
def copyDeployableFileToFailed_Check() {
    // To check again if the deployable file was copied (in case of command failure without exit code)
    fileFound = powershell (script: "Test-Path -Path ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}", returnStdout: true).trim()

    // If the file is found
    if (fileFound == "True") return true
    else return false
}
def copyDeployableFileToFailed_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file could not be copied from Inbox folder to Failed folder, please copy it manually. Name of the destination file: ${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}")
}

// Functions to copy the deployable file from Inbox folder to Partial-Deployed folder in the Deployment Server
def copyDeployableFileToPartialDeployed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyDeployableFileToPartialDeployed_Task
    check = this.&copyDeployableFileToPartialDeployed_Check
    ifFailure = this.&copyDeployableFileToPartialDeployed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyDeployableFileToPartialDeployed_Task() {
    // To copy the deployable file from Inbox folder to Partial-Deployed folder in Deployment Server
    powershell "Copy-Item -Path ${inboxFolderPath}\\${deployableFileName} -Destination ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}"
}
def copyDeployableFileToPartialDeployed_Check() {

    // To check if the deployable file was copied
    fileFound = powershell (script: "Test-Path -Path ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}", returnStdout: true).trim()

    // If the file is found
    if (fileFound == "True") return true
    else return false
}
def copyDeployableFileToPartialDeployed_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file could not be copied from Inbox folder to Partial-Deployed folder, please copy it manually. Name of the destination file: ${environmentParam}-build-$BUILD_NUMBER-${deployableFileName}")
}

// Functions to copy the hash file from Inbox folder to Deployed folder in the Deployment Server
def copyHashFileToDeployed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyHashFileToDeployed_Task
    check = this.&copyHashFileToDeployed_Check
    ifFailure = this.&copyHashFileToDeployed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyHashFileToDeployed_Task() {
    // To copy the hash file from Inbox folder to Deployed folder in Deployment Server
    powershell "Copy-Item -Path ${inboxFolderPath}\\${verifiedHashFileName} -Destination ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}"
}
def copyHashFileToDeployed_Check() {
    // To check again if the hash file was copied (in case of command failure without exit code)
    fileFound = powershell (script: "Test-Path -Path ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}", returnStdout: true).trim()

    // If the file is found
    if (fileFound == "True") return true
    else return false
}
def copyHashFileToDeployed_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The hash file could not be copied from Inbox folder to Deployed folder, please copy it manually. Name of the destination file: ${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}")
}

// Functions to copy the hash file from Inbox folder to Failed folder in the Deployment Server
def copyHashFileToFailed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyHashFileToFailed_Task
    check = this.&copyHashFileToFailed_Check
    ifFailure = this.&copyHashFileToFailed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyHashFileToFailed_Task() {
    // To copy the hash file from Inbox folder to Failed folder in Deployment Server
    powershell "Copy-Item -Path ${inboxFolderPath}\\${verifiedHashFileName} -Destination ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}"
}
def copyHashFileToFailed_Check() {
    // To check again if the hash file was copied (in case of command failure without exit code)
    fileFound = powershell (script: "Test-Path -Path ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}", returnStdout: true).trim()

    // If the file is found
    if (fileFound == "True") return true
    else return false
}
def copyHashFileToFailed_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The hash file could not be copied from Inbox folder to Failed folder, please copy it manually. Name of the destination file: ${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}")
}

// Functions to copy the hash file from Inbox folder to Partial-Deployed folder in the Deployment Server
def copyHashFileToPartialDeployed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&copyHashFileToPartialDeployed_Task
    check = this.&copyHashFileToPartialDeployed_Check
    ifFailure = this.&copyHashFileToPartialDeployed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def copyHashFileToPartialDeployed_Task() {
    // To copy the hash file from Inbox folder to Partial-Deployed folder in Deployment Server
    powershell "Copy-Item -Path ${inboxFolderPath}\\${verifiedHashFileName} -Destination ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}"
}
def copyHashFileToPartialDeployed_Check() {
    // To check again if the hash file was copied (in case of command failure without exit code)
    fileFound = powershell (script: "Test-Path -Path ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}", returnStdout: true).trim()

    // If the file is found
    if (fileFound == "True") return true
    else return false
}
def copyHashFileToPartialDeployed_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The hash file could not be copied from Inbox folder to Partial-Deployed folder, please copy it manually. Name of the destination file: ${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}")
}

// Function to deploy to each server using the commands acoording to the deploy type
def deploy(server, commandsArray) {
    return {
        
        int commandCounter
        int commandTries
        Boolean commandSucessful
        Boolean continueloop

        // To create a separated stage for each server
        stage("Deploy to ${server}") {           

            // Loop for execute all commands on the servers
            continueloop = true
            // To begin from the first command of the list
            commandCounter = 0

            // To reset the counter of tries for each command
            commandTries = 0
            
            // To repeat the command for a number of times
            while (continueloop == true) {

                stage("Executing ${commandsArray[commandCounter]}") {

                    // To increase the counter of tries for each command
                    commandTries++

                    // To reset the command controller as if the command is sucessful, because if it is not sucessful it will entry the catch and will set the controller as unsucessful
                    commandSucessful = true

                    // Show the command and the attempt on the console
                    echo ">>>>> Command: ${commandsArray[commandCounter]} Attempt: ${commandTries}"

                    try {
                        // Command line of the Deployment Tool according to the documentation (it is as echo now but when the Deployment Tool is set up, change it to powershell using Invoke-Command)
                        echo "C:\\Program Files\\SchneiderElectric\\DeploymentTool\\3.8.3\\bin\\DeploymentTool.exe AD -system ${server} -mode MyDMS -pass <password> -email name1.lastname1@schneider-electric-dms.com name2.lastname2@schneider-electric-dms.com -configuration ${commandsArray[commandCounter]} -environment InstallationDir:${inboxFolderPath}\\${deployableFileName}"

                        // Fail the command within the failure percentage (only for test purposes, on production set failurePercentage to 0)
                        //randomNumber1to100 = Math.abs(new Random().nextInt() % 100) + 1
                        //if (randomNumber1to100 < failurePercentage){
                        //    powershell (script: "Test-Connection 172.31.0.184 -ErrorAction Stop",returnStdout: true).trim()
                        //}
                    }
                    catch (Exception e1) {
                    
                        // Show that the command failed on the console
                        echo ">>>>> Failed"

                        // Set the command controller as unsucessful
                        commandSucessful = false

                        // If Jenkins tried to execute the command for less than 5 times
                        if (commandTries < 5) {
                        
                            // Wait before trying again
                            sleep(time:sleepTimeSeconds,unit:"SECONDS")
                        }
                        else {
                        
                            // Put the server on a unsucessful deployment list, that will be shown in the end of the pipeline
                            unsucessfulDeploymentArray.push(server)

                            // Send an email to responsible team
                            echo "Failed to execute the command ${commandsArray[commandCounter]} and the following on the server ${server}"
                        }
                    }

                    // If the command controler is still sucessfull
                    if (commandSucessful == true) {
                    
                        // Show that the command was successful on the console
                        echo ">>>>> Success"

                        // If the last command of the list is already executed
                        if (commandCounter == commandsArray.size()){
                        
                            // Send an email to responsible team and register in the console
                            sendSuccess("Deployment sucessfull on server ${server}")

                            // Start Smoke Tests Pipeline
                            //build job: 'adms-smoke-tests'
                        
                            // Exit the loop so no other command is executed
                            continueloop == false
                        }
                        else {

                            // Increase the command counter, so in the next loop the next command of the list will be executed
                            commandCounter ++

                            // Reset the command tries, so the system will try to execute the next command for 5 times or less
                            commandTries = 0
                        }
                    }
                }    
            }
        }
    }   
}

// Function to end the pipeline as failed, sending an email with a message received as parameter and registering in Jenkins console the same message
def failPipeline(message) {
    //sendFailureEmail(message)
    error(message)
}

// Functions to remove the deployable file from Inbox folder of Deployment Server
def removeDeployableFile() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&removeDeployableFile_Task
    check = this.&removeDeployableFile_Check
    ifFailure = this.&removeDeployableFile_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def removeDeployableFile_Task() {
    // To remove the deployable file from Inbox folder of Deployment Server
    powershell "Remove-Item ${inboxFolderPath}\\${deployableFileName} -Recurse -Force"
}
def removeDeployableFile_Check() {
    // To check again if the deployable file was removed from Inbox folder of Deployment Server (in case of command failure without exit code)
    fileFound = powershell (script: "Test-Path -Path ${inboxFolderPath}\\${deployableFileName}", returnStdout: true).trim()

    // If the file is not found
    if (fileFound == "False") return true
    else return false
}
def removeDeployableFile_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file could not be removed from the Inbox folder of Deployment server, please remove it manually")
}

// Functions to remove the hash file from Inbox folder of Deployment Server
def removeHashFile() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&removeHashFile_Task
    check = this.&removeHashFile_Check
    ifFailure = this.&removeHashFile_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def removeHashFile_Task() {
    // To remove the hash file from Inbox folder of Deployment Server
    powershell "Remove-Item ${inboxFolderPath}\\${verifiedHashFileName} -Recurse -Force"
}
def removeHashFile_Check() {
    // To check again if the hash file was removed from Inbox folder of Deployment Server (in case of command failure without exit code)
    fileFound = powershell (script: "Test-Path -Path ${inboxFolderPath}\\${verifiedHashFileName}", returnStdout: true).trim()

    // If the file is not found
    if (fileFound == "False") return true
    else return false
}
def removeHashFile_IfFailure() {
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The hash file could not be removed from the Deployment server, please remove it manually")
}

// Function to send an alert by email with a message received as parameter and registering in Jenkins console the same message
def sendAlert(message) {
    //sendFailureEmail(message)
    echo(message)
}

def sendEnvironmentVariableFail(environmentVariableName) {

    generalMessage = ("The environment variable ${environmentVariableName} is missing on Jenkins configuration.\n\nThis configuration is necessary to proceed with this pipeline.\n\nAs detailed at Deployment Automation documentation, please access Jenkins configuration through Manage Jenkins >> Configure System >> Environment Variables and configure the environment variable.")
    
    // Send an email to responsible team
    echo "${generalMessage}\n\nThen, please access the build through the link below, hover the paused stage in front of number ${BUILD_NUMBER}, type 1 to try again and click Proceed. You can also cancel the build typing 0 and clicking Proceed."

    // Ask the user to try again or end the pipeline
    tryAgain = input message: "${generalMessage}\n\nThen, please return here and type 1 to try again or 0 to end the pipeline, than click Proceed.", parameters: [string(defaultValue: '', description: '', name: '')]
}

// Function to send an email to responsible team reporting a failure
def sendFailureEmail(message) {
    mail to: env.emailRecipients,
    subject: "Failure on pipeline ${JOB_NAME}, build ${BUILD_NUMBER}",
    body: "Failure message: ${message}\n\nPipeline: ${JOB_NAME}\n\nBuild: ${BUILD_NUMBER}\n\nEnvironment: ${environmentParam}\n\nLink: ${JOB_URL}"
}

// Function to send an success message by email with a message received as parameter and registering in Jenkins console the same message
def sendSuccess(message) {
    echo(message)
}

// Function to send an email to responsible team reporting a success
def sendSuccessEmail(message) {
    mail to: env.emailRecipients,
    subject: "Success on pipeline ${JOB_NAME}, build ${BUILD_NUMBER}",
    body: "Success message: ${message}\n\nPipeline: ${JOB_NAME}\n\nBuild: ${BUILD_NUMBER}\n\nEnvironment: ${environmentParam}\n\nLink: ${JOB_URL}"
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

// Functions to verify the hash of the deployable file on Deployed folder
def verifyHashDeployed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&verifyHashDeployed_Task
    check = this.&verifyHashDeployed_Check
    ifFailure = this.&verifyHashDeployed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def verifyHashDeployed_Task() {
    
    // To get the hash that is inside the .txt file on the Inbox folder
    verifiedHashValue = powershell (script: "type ${inboxFolderPath}\\${verifiedHashFileName} -ErrorAction Stop", returnStdout: true).trim()

    // To calculate the hash of the deployable file on the Deployed folder
    calculatedHashValue = powershell (script: "(Get-FileHash ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} -ErrorAction Stop).Hash", returnStdout: true).trim()
}
def verifyHashDeployed_Check() {
    // To check if the calculated hash is equals to verified hash
    if(calculatedHashValue == verifiedHashValue) return true
    else return false
}
def verifyHashDeployed_IfFailure() {
    // To remove the deployable file from Deployed folder
    powershell "Remove-Item ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} -Recurse -Force"

    // To remove the hash file from Deployed folder
    powershell "Remove-Item ${deployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName} -Recurse -Force"
    
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file ${deployableFileName} and hash file ${verifiedHashFileName} could not be copied from ${inboxFolderPath} to ${deployedFolderPath}\nDeployable file and hash file don't match after copying.\nPlease copy them manually.\nName of the destination files: ${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} and ${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}")
}

// Functions to verify the hash of the deployable file in the Failed folder
def verifyHashFailed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&verifyHashFailed_Task
    check = this.&verifyHashFailed_Check
    ifFailure = this.&verifyHashFailed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def verifyHashFailed_Task() {
    // To get the hash that is inside the .txt file on the Inbox folder
    verifiedHashValue = powershell (script: "type ${inboxFolderPath}\\${verifiedHashFileName} -ErrorAction Stop", returnStdout: true).trim()

    // To calculate the hash of the deployable file on the Failed folder
    calculatedHashValue = powershell (script: "(Get-FileHash ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} -ErrorAction Stop).Hash", returnStdout: true).trim()
}
def verifyHashFailed_Check() {
    // To check if the calculated hash is equals to verified hash
    if(calculatedHashValue == verifiedHashValue) return true
    else return false
}
def verifyHashFailed_IfFailure() {
    // To remove the deployable file from Failed folder
    powershell "Remove-Item ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} -Recurse -Force"

    // To remove the hash file from Failed folder
    powershell "Remove-Item ${failedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName} -Recurse -Force"
    
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file and hash file could not be copied from Inbox folder to Failed folder.\nDeployable file and hash file don't match after copying.\nPlease copy them manually.\nName of the destination files: ${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} and ${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}")
}

// Functions to verify the hash of the deployable file on Partial-Deployed folder
def verifyHashPartialDeployed() {
    // To execute the function tryManyTimes passing as parameter: function to execute, function to check and function to execute in case of failure after trying many times
    task = this.&verifyHashPartialDeployed_Task
    check = this.&verifyHashPartialDeployed_Check
    ifFailure = this.&verifyHashPartialDeployed_IfFailure
    tryManyTimes(task,check,ifFailure)
}
def verifyHashPartialDeployed_Task() {
    
    // To get the hash that is inside the .txt file on the Inbox folder
    verifiedHashValue = powershell (script: "type ${inboxFolderPath}\\${verifiedHashFileName} -ErrorAction Stop", returnStdout: true).trim()

    // To calculate the hash of the deployable file on the Partial-Deployed folder
    calculatedHashValue = powershell (script: "(Get-FileHash ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} -ErrorAction Stop).Hash", returnStdout: true).trim()
}
def verifyHashPartialDeployed_Check() {
    // To check if the calculated hash is equals to verified hash
    if(calculatedHashValue == verifiedHashValue) return true
    else return false
}
def verifyHashPartialDeployed_IfFailure() {
    // To remove the deployable file from Partial-Deployed folder
    powershell "Remove-Item ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} -Recurse -Force"

    // To remove the hash file from Partial-Deployed folder
    powershell "Remove-Item ${partialDeployedFolderPath}\\${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName} -Recurse -Force"
    
    // Send an alert message to the user, without ending the pipeline as failed
    sendAlert("The deployable file ${deployableFileName} and hash file ${verifiedHashFileName} could not be copied from ${inboxFolderPath} to ${partialDeployedFolderPath}\nDeployable file and hash file don't match after copying.\nPlease copy them manually.\nName of the destination files: ${environmentParam}-build-$BUILD_NUMBER-${deployableFileName} and ${environmentParam}-build-$BUILD_NUMBER-${verifiedHashFileName}")
}

pipeline {
    agent any

    // To get the type of deployment that was passed as param from the "Copy" pipeline
    parameters {
        string(name: 'deployParam')
    }
    stages {
        stage('To check connection, parameters and variables'){
            steps{
                script{

                    // To check connection with Deployment Server
                    checkConnection(deploymentServerIp)

                    // To check the emailRecipients environment variable
                    //checkEnvironmentVariable_emailRecipients()

                    // To check if the type of deployment was received as a parameter
                    checkParameter("deployment type",params.deployParam)
                    
                    // To check if there is a list of commands for the type of deployment
                    checkListOfCommands()
                }
            }
        }
        stage('Start deployment'){
            steps{
                script{

                    // To check if there is a valid list of commands
                    if (commandsList != null){

                        // To transform the list of commands from string (Jenkins configuration) to array (manipulable by the deploy function above)
                        commandsArray = Eval.me(commandsList)

                        // To reset the variable that tries again if the lists are missing
                        tryAgain = "1"

                        // To start the loop of checking the list of servers
                        while (tryAgain == "1"){

                            // To check if there is a list of servers for the type of environment. For the PoC the environment will be only DEV
                            if (environmentParam == "DEV") {
                                
                                // If the dev commands is missing in Jenkins configuration
                                if (env.devServers == null) {                                        
                                    
                                    // Send an email to responsible team
                                    echo "The list of servers for ${environmentParam} environment is missing.\n\nAs detailed at Deployment Automation documentation, please access Jenkins configuration through Manage Jenkins >> Configure System >> Environment Variables and configure the environment variable for ${environmentParam} environment.\n\nThen, please access the build through the link below, hover the paused stage in front of number ${BUILD_NUMBER}, type 1 to try again and click Proceed. You can also cancel the build typing 0 and clicking Proceed."

                                    // Ask the user to try again or end the pipeline
                                    tryAgain = input message: "The list of servers is missing for the ${environmentParam} environment. Please type 1 to try again or 0 to end the pipeline", parameters: [string(defaultValue: '', description: '', name: '')]
                                }
                                else {
                                    
                                    // The servers that will be deployed are the DEV servers
                                    serversList = env.devServers
                                    
                                    // To end the loop
                                    tryAgain = "0"
                                }
                            }
                        }

                        // To check if there is a valid list of servers
                        if (serversList != null){

                            // To get the name of the deployable file
                            deployableFileName = powershell (script: "(Get-ChildItem -Path \"${inboxFolderPath}\" -recurse -exclude *.txt).Name", returnStdout: true).trim()
                            
                            // To transform the list of servers from string (Jenkins configuration) to array (manipulable by the deploy function above)
                            serversArray = Eval.me(serversList)

                            // For each server of list of servers, call the deploy function
                            parallelDeployments = serversArray.collectEntries {
                                ["Deploy to ${it}" : deploy(it, commandsArray)]
                            }

                            // To capture any error, so the Environment pipeline doesn't end if any of the parallel jobs fails
                            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                                
                                // Each deploy of the previous instructions will executed in parallel (at the same time) for all the servers
                                parallel parallelDeployments
                            }
                        }
                    }
                }
            }
        }
        stage('Post deployment'){
            steps{
                script{

                    // To check if there is a valid list of commands
                    if (commandsList != null){

                        // To check if there is a valid list of servers
                        if (serversList != null){

                            // To get the name of the verified hash file
                            verifiedHashFileName = powershell (script: "(Get-ChildItem -Path \"${inboxFolderPath}\" -recurse -Filter *.txt).Name", returnStdout: true).trim()

                            // If there is no unsucessfull deployment
                            if (unsucessfulDeploymentArray.size() == 0){
                                
                                // Send an email to responsible team and register in console
                                sendSuccess("Deploy completed sucessfully on all servers")

                                // To copy the deployable file from Inbox folder to Deployed folder on Deployment Server
                                copyDeployableFileToDeployed()
                                // To copy the hash file from Inbox folder to Deployed folder on Deployment Server
                                copyHashFileToDeployed()
                                // Verify hash of the deployable file on the Deployed folder
                                verifyHashDeployed()
                                // To remove the deployable file from Inbox folder on Deployment Server
                                removeDeployableFile()
                                // To remove the hash file from Inbox folder on Deployment Server
                                removeHashFile()



                            }
                            else if (unsucessfulDeploymentArray.size() == serversArray.size()){
                                
                                // Send an email to responsible team
                                echo "Failed to deploy on the following servers: ${unsucessfulDeploymentArray.join(", ")}"

                                // To copy the deployable file from Inbox folder to Failed folder on Deployment Server
                                copyDeployableFileToFailed()
                                // To copy the hash file from Inbox folder to Failed folder on Deployment Server
                                copyHashFileToFailed()
                                // Verify hash of the deployable file on the Failed folder
                                verifyHashFailed()

                                // To check if the calculated hash is equals to verified hash
                                if(calculatedHashValue == verifiedHashValue){
                                    
                                    // To remove the deployable file from Inbox folder on Deployment Server
                                    removeDeployableFile()
                                    // To remove the hash file from Inbox folder on Deployment Server
                                    removeHashFile()
                                }
                                else failPipeline("Deployable file and hash file don't match")
                            }
                            else {
                                
                                // Send an email to responsible team
                                echo "Failed to deploy on the following servers: ${unsucessfulDeploymentArray.join(", ")}"

                                // To copy the deployable file from Inbox folder to Partial-Deployed folder on Deployment Server
                                copyDeployableFileToPartialDeployed()
                                // To copy the hash file from Inbox folder to Partial-Deployed folder on Deployment Server
                                copyHashFileToPartialDeployed()
                                // Verify hash of the deployable file on the Partial-Deployed folder
                                verifyHashPartialDeployed()

                                // To check if the calculated hash is equals to verified hash
                                if(calculatedHashValue == verifiedHashValue){
                                    
                                    // To remove the deployable file from Inbox folder on Deployment Server
                                    removeDeployableFile()
                                    // To remove the hash file from Inbox folder on Deployment Server
                                    removeHashFile()
                                }
                                else failPipeline("Deployable file and hash file don't match")
                            }
                        }
                    }
                }
            }
        }
    }
}