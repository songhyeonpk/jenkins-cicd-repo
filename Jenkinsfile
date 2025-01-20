pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                // Gradle Build
                sh './gradlew clean build'
            }
        }

        stage('test') {
            steps {
                // 테스트 실행
                sh './gradlew test'
            }
        }

        stage('package') {
            steps {
                // JAR 파일 생성 확인
                sh 'ls build/libs/*.jar'
            }
        }
    }

    post {
        always {
            // 빌드 후 로그 정리 및 결과 아카이빙
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
        }
        success {
            echo 'Build succeeded.'
        }
        failure {
            echo 'Build failed.'
        }
    }
}