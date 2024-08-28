#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_mimer = "${bob} -r ruleset2.0.mimer.yaml"

try {
    stage('Mimer Init') {
        if (env.MUNIN_UPDATE_ENABLED == "true" || env.RELEASE == "true") {
            sh "${bob_mimer} mimer-init"
        }
    }
} catch (e) {
    throw e
}