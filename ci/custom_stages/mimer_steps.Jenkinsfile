#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_mimer = "${bob} -r ruleset2.0.mimer.yaml"

if (env.RELEASE == "true") {
    try {
        stage('Store release images artifacts') {
            sh "${bob_mimer} image-package-release"
            sh "${bob_mimer} publish-released-docker-image"
            archiveArtifacts "build/released-images/*.tar.gz"
        }

        stage('Store release helm charts artifacts') {
            withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                sh "${bob_mimer} helm-package-release"
                sh "${bob_mimer} helm-publish-release"
            }
            archiveArtifacts "build/released-charts/*.tgz"
        }

        stage('Push plus git-tag') {
            withCredentials([usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')]) {
                sh "${bob_mimer} create-plus-git-tag"
                sh "git pull"
            }
        }

        // Commenting the munin stages - error is "lifecycleStage - An ERICSSON source or derivative PV cannot be released when tradeComplianceStatus is either not defined or set to InWork."
        // stage('[MUNIN] Munin Update') {
        //     withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
        //         sh "${bob_mimer} munin-update-version"
        //     }
        // }

        // stage('[MUNIN] Fetch artifact checksums') {
        //     withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
        //         // Fetch checksums of docker images and helm charts
        //         sh "${bob_mimer} fetch-artifact-checksums"
        //     }
        // }

        // stage('[MUNIN] Set artifact in Munin') {
        //     withCredentials([usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GITCA_USERNAME', passwordVariable: 'GITCA_PASSWORD')]) {
        //         // Set artifact URLs in PLMS via GitCA
        //         sh "${bob_mimer} munin-connect-ca-artifact"
        //     }
        //     withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
        //         // Set artifact urls in Munin
        //         sh "${bob_mimer} munin-set-artifact"
        //     }
        // }

        // stage('[MUNIN] Release product in Munin') {
        //     withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
        //         // Release products in Munin
        //         sh "${bob_mimer} munin-release-version"
        //     }
        // }
    } catch (e) {
        throw e
    }
}