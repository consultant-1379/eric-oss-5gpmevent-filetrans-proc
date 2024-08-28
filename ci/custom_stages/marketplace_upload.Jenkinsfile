#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_custom = "${bob} -r ruleset2.0.yaml"
def bob_protobuf = "${bob} -r ruleset_protobuf_2.0.yaml"

try {
    stage('Marketplace Upload') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
            string(credentialsId: 'MARKETPLACE_TOKEN_5GPM', variable: 'MARKETPLACE_TOKEN')
        ]) {
            sh "${bob_custom} generate-doc-zip-package"
            if (env.RELEASE != "true") {
                sh "${bob_protobuf} put-to-dev-generic-local"
                sh "${bob_protobuf} update-dev-marketplace_upload_config"
                sh "${bob_custom} marketplace-upload:upload-doc-to-arm-dev"
            }
            if (env.RELEASE == "true") {
                sh "${bob_protobuf} put-to-released-generic-local"
                sh "${bob_protobuf} update-release-marketplace_upload_config"
                sh "${bob_custom} marketplace-upload:upload-doc-to-arm-released"
            }
            sh "${bob_custom} marketplace-upload:refresh-adp-portal-marketplace"
        }
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts "build/doc-marketplace/**"
    archiveArtifacts "build/doc-svl-replacement/**"
    archiveArtifacts "src/main/resources/event-mapping.yaml"
    archiveArtifacts "build/doc-marketplace-protobuf/**"
}