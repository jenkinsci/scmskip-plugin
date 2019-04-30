pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                scmSkip(skipPattern: '.*\\[(ci skip|skip ci)\\].*')
            }
        }
    }
}