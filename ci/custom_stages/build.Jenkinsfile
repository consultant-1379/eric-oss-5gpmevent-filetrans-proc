#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_custom = "${bob} -r ruleset2.0.yaml"

try {
    stage('Build') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob_custom} build"
        }
    }
} catch (e) {
    throw e
}