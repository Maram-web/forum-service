pipeline {
    agent any

    environment {
        TIMESTAMP = "${new Date().format('yyyyMMdd-HHmmss')}"
        IMAGE_TAG = "v${TIMESTAMP}"
        IMAGE_NAME = "marammanai/forum-service:${IMAGE_TAG}"
        K8S_MASTER = "ceph1@192.168.13.11"
        DEPLOY_YAML = "k8s-forum-deployment.yaml"
    }


    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh """
                docker build -t ${IMAGE_NAME} .
                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                docker push ${IMAGE_NAME}
                """
            }
        }

        stage('Deploy to K8s') {
            steps {
                sh """
                scp ${DEPLOY_YAML} ${K8S_MASTER}:/home/ceph1/
                ssh ${K8S_MASTER} "sed -i 's|__IMAGE_TAG__|${IMAGE_TAG}|g' /home/ceph1/${DEPLOY_YAML} && kubectl apply -f /home/ceph1/${DEPLOY_YAML}"
                """
            }
        }
    }
}
