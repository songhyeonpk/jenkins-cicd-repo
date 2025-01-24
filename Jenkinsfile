pipeline {
    agent any

    options {
        timeout(time: 5, unit: 'MINUTES')   // timeout 5분 설정
    }

    environment {
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"   // Gradle 캐시 설정
        DOCKER_IMAGE = 'jenkins-test:latest'
    }

    stages {
        // 체크아웃
        stage('Checkout Source') {
            steps {
                script {
                    echo "Checking out branch: ${env.BRANCH_NAME}"

                    // 멀티브랜치 파이프라인 -> 현재 빌드 중인 브랜치를 자동으로 감지하고 체크아웃
                    checkout scm
                }
            }
        }

        // CI : Gradle 빌드 및 테스트, 도커 이미지 빌드 및 push
        stage('CI - Build and Test, Docker Image Build and Push') {
            // 모든 브랜치 PR 이벤트 시 실행
            when {
                expression {
                    def isPR = env.CHANGE_ID != null    // PR 이벤트 여부 (true: PR, false: 다른 이벤트)
                    return isPR
                }
            }
            steps {
                script {
                    echo "Running CI pipeline... for branch: ${env.BRANCH_NAME}"

                    // Gradle 테스트 실행 및 빌드 성공 여부 확인
                    sh './gradlew clean build --no-daemon'

                    // 도커 이미지 빌드 및 push
                    withCredentials([
                            usernamePassword(
                                    credentialsId: 'docker-credentials',
                                    usernameVariable: 'DOCKERHUB_USERNAME',
                                    passwordVariable: 'DOCKERHUB_ACCESS_TOKEN'
                            )
                    ]) {
                        def dockerImage = "${DOCKERHUB_USERNAME}/${env.DOCKER_IMAGE}"

                        sh """
                        # 도커 로그인
                        echo \$DOCKERHUB_ACCESS_TOKEN | docker login -u \$DOCKERHUB_USERNAME --password-stdin

                        # 도커 이미지 빌드
                        docker build -t $dockerImage .

                        # 도커 이미지 push
                        docker push $dockerImage
                        """
                    }
                }
            }
        }

        // CD : SSH EC2 연결 및 배포
        stage('CD - Deploy') {
            // develop, main 브랜치의 push 이벤트 시 작업 수행
            when {
                expression {
                    def branch = env.BRANCH_NAME
                    return ['develop', 'main'].contains(branch) && env.CHANGE_ID == null    // develop, main 브랜치 push 이벤트
                }
            }
            steps {
                script {
                    echo "Running CD pipeline... for branch: ${env.BRANCH_NAME}"

                    withCredentials([
                            usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_ACCESS_TOKEN'),
                            string(credentialsId: 'ec2-user', variable: 'EC2_USER'),
                            string(credentialsId: 'ec2-host', variable: 'EC2_HOST')]) {
                        // 도커 이미지
                        def dockerImage = "${DOCKERHUB_USERNAME}/${env.DOCKER_IMAGE}"
                        // 도커 컨테이너
                        def dockerContainerName = "test-server-container"

                        // SSH EC2 연결
                        sshagent(['ssh-credentials']) {
                            sh """
                            ssh -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST << EOF
                                set -e
                                echo "Conneted to EC2"
                                
                                # 배포환경 도커 로그인
                                echo \$DOCKERHUB_ACCESS_TOKEN | docker login -u \$DOCKERHUB_USERNAME --password-stdin
                                
                                # 도커 이미지 pull
                                docker pull $dockerImage

                                # 기존 도커 컨테이너 중지 및 삭제
                                docker stop $dockerContainerName 2>/dev/null || true
                                docker rm $dockerContainerName 2>/dev/null || true

                                # 도커 컨테이너 실행
                                docker run -d --name $dockerContainerName -p 8080:8080 $dockerImage

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