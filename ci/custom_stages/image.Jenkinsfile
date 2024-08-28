#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_common = "${bob} -r ci/common_ruleset2.0.yaml"

try {
    stage('Image') {
        ci_pipeline_scripts.retryMechanism("${bob_common} image", 3)
        ci_pipeline_scripts.retryMechanism("${bob_common} image-dr-check", 3)
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/image-design-rule-check-report*'
}