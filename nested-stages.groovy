pipeline {
    agent none

    stages {
        stage("1") {
            parallel {
                stage("1.1") {
                    stages {
                        stage("1.1.1") {
                            steps {
                                echo "1.1.1"
                            }
                        }
                        stage("1.1.2") {
                            steps {
                                echo "1.1.2"
                            }
                        }
                    }
                }
                stage("1.2") {
                    stages {
                        stage("1.2.1") {
                            steps {
                                echo "1.2.1"
                            }
                        }
                        stage("1.2.2") {
                             steps {
                                echo "1.2.2"
                            }
                        }
                    }
                }
            }
        }
    }
}