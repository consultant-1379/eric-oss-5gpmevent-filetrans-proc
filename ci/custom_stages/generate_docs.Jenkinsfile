#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_custom = "${bob} -r ruleset2.0.yaml"

try {
    stage('Generate') {
        sh "${bob_custom} generate-docs"
        archiveArtifacts "build/doc/**/*.*"
    }
} catch (e) {
    throw e
}