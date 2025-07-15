pipeline {
    agent any

    environment {
        TIMESTAMP = "${new Date().format('yyyyMMdd-HHmmss')}"
        IMAGE_TAG = "v${TIMESTAMP}"
        IMAGE_NAME = "marammanai/forum-service:${IMAGE_TAG}"
        K8S_MASTER = "ceph1@192.168.13.11"
        DEPLOY_YAML = "k8s-forum-deployment.yaml"
        DEPLOY_YAML_TEMPLATE = "k8s-forum-template.yaml"
    }

    stages {
        stage('📦 Checkout') {
            steps {
                git branch: 'forum', url: 'https://github.com/Maram-web/forum-service.git'
            }
        }

        stage('🐳 Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh """
                        docker build -t ${IMAGE_NAME} .
                        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                        docker push ${IMAGE_NAME}
                    """
                }
            }
        }

        stage('📄 Prepare YAML') {
            steps {
                sh """
                    sed "s|__IMAGE_TAG__|${IMAGE_TAG}|g" ${DEPLOY_YAML_TEMPLATE} > ${DEPLOY_YAML}
                """
            }
        }

        stage('🚀 Deploy to Kubernetes') {
            steps {
                sh """
                    ssh-keyscan -H 192.168.13.11 >> ~/.ssh/known_hosts
                    scp ${DEPLOY_YAML} ${K8S_MASTER}:/home/ceph1/
                    ssh ${K8S_MASTER} kubectl apply -f /home/ceph1/${DEPLOY_YAML}
                """
            }
        }
    }

    post {
        success {
            echo "✅ forum-service déployé avec succès : ${IMAGE_TAG}"
        }
        failure {
            echo "❌ Échec du déploiement de forum-service"
        }
    }
}
