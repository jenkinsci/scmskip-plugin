pipeline {
    agent none

    stages {
        stage('Build') {
            steps {
                echo "before skip"
                scmSkip(skipPattern: '.*\\[(ci skip|skip ci)\\].*')
                echo "after skip"
            }
        }
    }
}