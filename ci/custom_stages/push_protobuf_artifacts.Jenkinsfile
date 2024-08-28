#!/usr/bin/env groovy

def bob = "./bob/bob"
def bob_protobuf = "${bob} -r ruleset_protobuf_2.0.yaml"

if (env.RELEASE == "true") {
    try {
        stage('Push Protobuf Artifacts') {
            withCredentials([usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')]) {
                script {
                    sh 'pwd'
                    def output = sh(returnStatus: true, script: 'git diff --quiet src/main/resources/event-mapping.yaml')
                    echo " The Output: '${output}'"
                    if (output == 1) {
                        echo 'Difference in event-mapping.yaml found, pushing new commit'
                        sh '''
                        git add src/main/resources/event-mapping.yaml &&
                        git commit -m "pushing event-mapping.yaml" &&
                        git push https://$GERRIT_USERNAME:$GERRIT_PASSWORD@gerrit-gamma.gic.ericsson.se/a/OSS/com.ericsson.oss.adc/eric-oss-5gpmevent-filetrans-proc HEAD:master
                        '''
                    } else if (output == 0) {
                        echo 'No difference in event-mapping.yaml found'
                    } else {
                        error("Failing job due to unexpected exit code from git diff command, exit code was: " + output)
                    }
                }
            }
        }
    } catch (e) {
        throw e
    }
}
