pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'before skip'
                scmSkip(skipPattern: '.*\\[(ci skip|skip ci)\\].*', deleteBuild: true)
                echo 'after skip'
            }
        }
    }
}