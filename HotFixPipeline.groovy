pipeline {
    agent any
   
    stages {
        
        stage("Start the pipeline") {
            steps {
                script {
                    for(int i=0; i < env.DevServers.size(); i++) {
                        stage(env.DevServers[i]){
                            echo "Element: ${env.DevServers[i]}"
                        }
                    }
                }
            }
        }
    }
}