#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_mimer = "${bob} -r ruleset2.0.mimer.yaml"

if (env.RELEASE != "true") {
    try {
        stage('FOSS Validation for Mimer') {
            if (env.MUNIN_UPDATE_ENABLED == "true") {
                withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                    sh "${bob_mimer} munin-update-version"
                }
            }
        }
    } catch (e) {
        throw e
    }
}