#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_protobuf = "${bob} -r ruleset_protobuf_2.0.yaml"

try {
    stage('Prepare Protobuf Artifacts') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
            usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')
        ]) {
            sh "${bob_protobuf} purge-radionode-dir"
            sh 'git clone https://${GERRIT_USERNAME}:${GERRIT_PASSWORD}@gerrit-gamma.gic.ericsson.se/a/OSS/com.ericsson.oss.mediation.model.nrm.function/radionode-node-model ' + " ${env.WORKSPACE}/doc/radionode-node-model"
            sh "${bob_protobuf} generate-proto-zips"
            sh "${bob_protobuf} test-proto-files"
            sh "${bob_protobuf} prepare-protobufs-for-marketplace"
            sh "${bob_protobuf} purge-radionode-dir"
        }
    }
} catch (e) {
    throw e
}