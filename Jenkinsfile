pipeline {
    agent any

    options {
        timeout(time: 5, unit: 'MINUTES')   // timeout 5분 설정
    }

    environment {
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"   // Gradle 캐시 설정
    }

    stages {
        // 환경 변수 준비
        stage('Prepare Environment') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        env.DOCKER_USERNAME = "${DOCKER_USERNAME}"
                        env.DOCKER_PASSWORD = "${DOCKER_PASSWORD}"
                        env.DOCKER_IMAGE = "${DOCKER_USERNAME}/jenkins-test:latest"
                    }
                }
            }
        }

        // Gradle 빌드
        stage('Build') {
            steps {
                // Gradle Build
                sh './gradlew clean build -x test'
            }
        }

        // 테스트 실행
        stage('Test') {
            steps {
                // 테스트 실행
                sh './gradlew test'
            }
        }

        // JAR 파일 패키징
        stage('Package') {
            steps {
                // JAR 파일 생성 확인
                sh 'ls build/libs/*.jar'
            }
        }

        // 도커 이미지 빌드
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                    docker build -t "${DOCKER_IMAGE}" .
                    """
                }
            }
        }

        // 도커 로그인 & 도커 이미지 push
        stage('Docker Login & Push Docker Image') {
            steps {
                script {
                    sh """
                    echo "\${DOCKER_PASSWORD}" | docker login -u "\${DOCKER_USERNAME}" --password-stdin
                    docker push "${DOCKER_IMAGE}"
                    """
                }
            }
        }

        // 배포
        stage('Deploy') {
            steps {
                script {
                    withCredentials([
                            string(credentialsId: 'ec2-user', variable: 'EC2_USER'),
                            string(credentialsId: 'ec2-host', variable: 'EC2_HOST')]) {
                        sshagent(['ssh-credentials']) {
                            def DOCKER_CONTAINER_NAME = "test-server-container"

                            sh"""
                            # SSH EC2 연결
                            ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} << EOF
                                set -e
                                echo "Connected to EC2"
                                
                                # 도커 로그인
                                echo "\$DOCKERHUB_ACCESS_TOKEN | docker login -u "\$DOCKERHUB_USERNAME --password-stdin
                                
                                # 도커 이미지 pull
                                docker pull $DOCKER_IMAGE

                                # 기존 도커 컨테이너 중지 및 삭제
                                docker stop $DOCKER_CONTAINER_NAME 2>/dev/null || true
                                docker rm $DOCKER_CONTAINER_NAME 2>/dev/null || true
                                
                                # 도커 컨테이너 실행
                                docker run -d --name $DOCKER_CONTAINER_NAME -p 8080:8080 $DOCKER_IMAGE

                                # 사용하지 않는 도커 이미지 모두 삭제
                                docker image prune -a -f
                            << EOF
                            """
                        }
                    }
                }
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