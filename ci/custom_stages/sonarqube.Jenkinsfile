#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_custom = "${bob} -r ruleset2.0.yaml"

try {
    stage('SonarQube') {
        if (env.SQ_ENABLED == "true" && env.RELEASE != "true") {
            withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                withSonarQubeEnv("${env.SQ_SERVER}") {
                    sh "${bob_custom} sonar-enterprise-pcr"
                }
            }
            timeout(time: 5, unit: 'MINUTES') {
                waitUntil {
                    withSonarQubeEnv("${env.SQ_SERVER}") {
                        script {
                            return ci_pipeline_scripts.getQualityGate()
                        }
                    }
                }
            }
        }
        else if (env.SQ_ENABLED == "true" && env.RELEASE == "true") {
            withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                withSonarQubeEnv("${env.SQ_SERVER}") {
                    sh "${bob_custom} sonar-enterprise-release"
                }
            }
        }
    }
} catch (e) {
    throw e
}