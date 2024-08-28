#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_common = "${bob} -r ci/common_ruleset2.0.yaml"

try {
    stage('Package') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
            file(credentialsId: 'docker-config-json', variable: 'DOCKER_CONFIG_JSON')
        ]) {
            ci_pipeline_scripts.checkDockerConfig()
            ci_pipeline_scripts.retryMechanism("${bob_common} package", 3)
            sh "${bob_common} delete-images-from-agent:delete-internal-image"
        }
    }
} catch (e) {
    throw e
}