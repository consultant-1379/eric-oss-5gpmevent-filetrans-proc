#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_custom = "${bob} -r ruleset2.0.yaml"
def bob_common = "${bob} -r ci/common_ruleset2.0.yaml"

try {
    stage('Helm Install') {
        sh "${bob_common} helm-dry-run"
        ci_pipeline_scripts.retryMechanism("${bob_custom} helm-install", 2)
        archiveArtifacts "doc/Characteristic_Test_Report/characteristic_test_report.md"
    }

    if (env.RELEASE == "true") {
        stage('K8S Restart Pod') {
            ci_pipeline_scripts.retryMechanism("${bob_custom} k8s-restart-pod", 3)
            archiveArtifacts "doc/Characteristic_Test_Report/characteristic_test_report.md"
        }
    }
} catch (e) {
    withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
        sh "${bob_common} collect-k8s-logs || true"
    }
    archiveArtifacts allowEmptyArchive: true, artifacts: "k8s-logs/*"
    sh "${bob_common} delete-namespace"
} finally {
    sh "${bob_common} kaas-info || true"
    archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
}