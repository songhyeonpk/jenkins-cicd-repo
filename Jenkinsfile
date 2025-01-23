pipeline {
    agent any

    options {
        timeout(time: 5, unit: 'MINUTES')   // timeout 5분 설정
    }

    environment {
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"   // Gradle 캐시 설정
    }

    stages {
        // 체크아웃
        stage('Checkout Source') {
            steps {
                script {
                    echo "Checking out branch: ${env.GIT_BRANCH}"

                    checkout([
                            $class: 'GitSCM',
                            branches: [[name: "${env.GIT_BRANCH}"]]
                    ])
                }
            }
        }

        // CI : Gradle 빌드 및 테스트
        stage('CI - Build and Test') {
            steps {
                script {
                    echo 'Running CI pipeline...'

                    // Gradle 테스트 실행 및 빌드 성공 여부 확인
                    sh './gradlew clean build --no-daemon'
                }
            }
        }

        // CD : 도커 이미지 빌드 및 배포
        stage('CD - Docker Image Build and Deploy') {
            // develop, main 브랜치에서만 작업 수행
            when {
                expression {
                    ['develop', 'main'].contains(env.GIT_BRANCH?.replaceFirst(/^origin\//, ''))
                }
            }
            steps {
                script {
                    echo 'Running CD pipeline...'

                    withCredentials([
                            usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_ACCESS_TOKEN'),
                            string(credentialsId: 'ec2-user', variable: 'EC2_USER'),
                            string(credentialsId: 'ec2-host', variable: 'EC2_HOST')]) {
                        // 도커 이미지
                        def DOCKER_IMAGE = '${DOCKERHUB_USERNAME}/jenkins-test:latest'
                        // 도커 컨테이너
                        def DOCKER_CONTAINER_NAME = "test-server-container"

                        // 도커 이미지 build & push
                        sh """
                        # 도커 로그인
                        echo \$DOCKERHUB_ACCESS_TOKEN | docker login -u \$DOCKERHUB_USERNAME --password-stdin
                        
                        # 도커 이미지 build
                        docker build -t $DOCKER_IMAGE .

                        # 도커 이미지 push
                        docker push $DOCKER_IMAGE
                        """

                        // SSH EC2 연결
                        sshagent(['ssh-credentials']) {
                            sh """
                            ssh -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST << EOF
                                set -e
                                echo "Conneted to EC2"
                                
                                # 배포환경 도커 로그인
                                echo \$DOCKERHUB_ACCESS_TOKEN | docker login -u \$DOCKERHUB_USERNAME --password-stdin
                                
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

    // 파이프라인 실행 후 수행할 작업 정의
    post {
        always {
            // 빌드 후 로그 정리 및 결과 아카이빙
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true

            // 슬랙 알림 전송
            echo 'Send slack alert.'
        }
        success {
            echo 'Pipeline succeeded.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}