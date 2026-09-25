// Jenkinsfile — build paralelo + push a Docker Hub + rollout restart en GKE
//
// Requiere en Jenkins (Manage Jenkins > Credentials):
//   - 'dockerhub-credentials'  (Username with password) -> cuenta contenedorbenjaxio
//   - 'gke-kubeconfig'        (Secret file) -> kubeconfig con acceso al cluster
//     smart-login-web (o credenciales para que `gcloud container clusters
//     get-credentials` funcione, si el agente tiene gcloud instalado)
//
// Trigger: push a main — requiere el plugin GitHub y el webhook configurado
// en el repo. Este Jenkinsfile asume un Multibranch Pipeline (usa
// env.BRANCH_NAME); si es un job simple de una sola rama, reemplaza el
// `when { branch 'main' }` por `when { expression { env.GIT_BRANCH ==~ /.*main/ } }`.

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    triggers {
        githubPush()
    }

    environment {
        DOCKER_REPO   = 'contenedorbenjaxio/smartlogix'
        GKE_PROJECT   = 'river-dynamo-502903-m6'
        GKE_CLUSTER   = 'smart-login-web'
        GKE_ZONE      = 'us-central1-a'
        K8S_NAMESPACE = 'smartlogix'
        SHORT_SHA     = "${env.GIT_COMMIT?.take(7)}"
    }

    stages {

        stage('Solo rama main') {
            when { branch 'main' }
            stages {

                stage('Docker Login') {
                    steps {
                        withCredentials([usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )]) {
                            sh 'echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin'
                        }
                    }
                }

                stage('Build & Push (paralelo)') {
                    parallel {
                        stage('gateway') {
                            steps { script { buildAndPush('Gateway', 'gateway') } }
                        }
                        stage('users') {
                            steps { script { buildAndPush('Users', 'users') } }
                        }
                        stage('rol') {
                            steps { script { buildAndPush('Rol', 'rol') } }
                        }
                        stage('estado') {
                            steps { script { buildAndPush('Estado', 'estado') } }
                        }
                        stage('inventario') {
                            steps { script { buildAndPush('Inventario', 'inventario') } }
                        }
                        stage('producto') {
                            steps { script { buildAndPush('Producto', 'producto') } }
                        }
                        stage('orden') {
                            steps { script { buildAndPush('Orden', 'orden') } }
                        }
                        stage('configuracion') {
                            steps { script { buildAndPush('Configuracion', 'configuracion') } }
                        }
                    }
                }

                stage('Deploy a GKE (rollout restart)') {
                    steps {
                        withCredentials([file(credentialsId: 'gke-kubeconfig', variable: 'KUBECONFIG')]) {
                            sh '''
                                gcloud container clusters get-credentials "$GKE_CLUSTER" \
                                    --zone "$GKE_ZONE" --project "$GKE_PROJECT" || true

                                kubectl rollout restart deployment/gateway               -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/users-service         -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/rol-service           -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/estado-service        -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/inventario-service    -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/producto-service      -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/orden-service         -n "$K8S_NAMESPACE"
                                kubectl rollout restart deployment/configuracion-service -n "$K8S_NAMESPACE"
                            '''
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
    }
}

// Construye y sube ambos tags (fijo + SHORT_SHA) de un servicio.
def buildAndPush(String dir, String tag) {
    sh """
        docker build -t ${env.DOCKER_REPO}:${tag} -t ${env.DOCKER_REPO}:${tag}-${env.SHORT_SHA} ./${dir}
        docker push ${env.DOCKER_REPO}:${tag}
        docker push ${env.DOCKER_REPO}:${tag}-${env.SHORT_SHA}
    """
}
