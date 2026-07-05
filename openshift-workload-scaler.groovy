pipeline {
    agent {
        label 'masterLin'
    }

    tools { oc 'oc-3.11' }

    parameters {
        choice(
                name: 'asName',
                choices: [
                        'SYSTEM-A-1',
                        'SYSTEM-A-2',
                        'SYSTEM-B-1',
                        'SYSTEM-B-2',
                        'SYSTEM-C-1',
                        'SYSTEM-C-2',
                        'SYSTEM-D-1',
                        'SYSTEM-D-2',
                        'SYSTEM-E-1',
                        'SYSTEM-E-2',
                        'SYSTEM-F-1',
                        'SYSTEM-F-2',
                        'SYSTEM-G-1',
                        'SYSTEM-G-2',
                        'SYSTEM-H-1',
                        'SYSTEM-H-2',
                        'SYSTEM-I-1',
                        'SYSTEM-I-2'
                ]
        )

        choice(
                name: 'asChoice',
                choices: ['0', '1'],
                description: '0 - Stop services, 1 - Start services'
        )

        choice(
                name: 'dryRun',
                choices: ['false', 'true'],
                description: 'true - simulate actions only, no changes applied'
        )

        string(name: 'nameUser', defaultValue: '', description: 'Username')
        password(name: 'secretUser', defaultValue: '', description: 'User password')
    }

    environment {
        REPLICAS_FILE = 'replicas_state.txt'
    }

    stages {
        stage('Scale PODS') {
            steps {
                wrap([$class: "MaskPasswordsBuildWrapper",
                      varPasswordPairs: [[password: params.nameUser],
                                         [password: params.secretUser]]]) {

                    script {

                        def SYSTEMS = [
                                'SYSTEM-A-1' : [url: 'example-app-a1.domain.local', server: 'https://api.example-a1.domain.local:6443', project: 'project-a1'],
                                'SYSTEM-A-2' : [url: 'example-app-a2.domain.local', server: 'https://api.example-a2.domain.local:6443', project: 'project-a2'],

                                'SYSTEM-B-1' : [url: 'example-app-b1.domain.local', server: 'https://api.example-b1.domain.local:6443', project: 'project-b1'],
                                'SYSTEM-B-2' : [url: 'example-app-b2.domain.local', server: 'https://api.example-b2.domain.local:6443', project: 'project-b2'],

                                'SYSTEM-C-1' : [url: 'example-app-c1.domain.local', server: 'https://api.example-c1.domain.local:6443', project: 'project-c1'],
                                'SYSTEM-C-2' : [url: 'example-app-c2.domain.local', server: 'https://api.example-c2.domain.local:6443', project: 'project-c2'],

                                'SYSTEM-D-1' : [url: 'example-app-d1.domain.local', server: 'https://api.example-d1.domain.local:6443', project: 'project-d1'],
                                'SYSTEM-D-2' : [url: 'example-app-d2.domain.local', server: 'https://api.example-d2.domain.local:6443', project: 'project-d2'],

                                'SYSTEM-E-1' : [url: 'example-app-e1.domain.local', server: 'https://api.example-e1.domain.local:6443', project: 'project-e1'],
                                'SYSTEM-E-2' : [url: 'example-app-e2.domain.local', server: 'https://api.example-e2.domain.local:6443', project: 'project-e2'],

                                'SYSTEM-F-1' : [url: 'example-app-f1.domain.local', server: 'https://api.example-f1.domain.local:6443', project: 'project-f1'],
                                'SYSTEM-F-2' : [url: 'example-app-f2.domain.local', server: 'https://api.example-f2.domain.local:6443', project: 'project-f2'],

                                'SYSTEM-G-1' : [url: 'example-app-g1.domain.local', server: 'https://api.example-g1.domain.local:6443', project: 'project-g1'],
                                'SYSTEM-G-2' : [url: 'example-app-g2.domain.local', server: 'https://api.example-g2.domain.local:6443', project: 'project-g2'],

                                'SYSTEM-H-1' : [url: 'example-app-h1.domain.local', server: 'https://api.example-h1.domain.local:6443', project: 'project-h1'],
                                'SYSTEM-H-2' : [url: 'example-app-h2.domain.local', server: 'https://api.example-h2.domain.local:6443', project: 'project-h2'],

                                'SYSTEM-I-1' : [url: 'example-app-i1.domain.local', server: 'https://api.example-i1.domain.local:6443', project: 'project-i1'],
                                'SYSTEM-I-2' : [url: 'example-app-i2.domain.local', server: 'https://api.example-i2.domain.local:6443', project: 'project-i2']
                        ]

                        currentBuild.displayName = "${params.asName}#${env.BUILD_ID}"

                        def config = SYSTEMS[params.asName]
                        if (!config) {
                            error "Unknown system: ${params.asName}"
                        }

                        def url = config.url
                        def server = config.server
                        def project = config.project

                        env.KUBECONFIG = "${env.WORKSPACE}/kubeconfig"

                        echo "Selected system: ${params.asName}"
                        echo "Dry-run mode: ${params.dryRun}"
                        echo "Target state for all pods: ${params.asChoice}"
                        echo "System URL: ${url}"
                        echo "API server: ${server}"
                        echo "Project: ${project}"

                        sh "oc login -u ${params.nameUser} -p ${params.secretUser} --server=${server} --insecure-skip-tls-verify=true"
                        sleep 5

                        if (!project || project == '__UNDEFINED_PROJECT__') {
                            error "Project is not defined for system ${params.asName}"
                        }

                        sh "oc project ${project}"
                        sleep 5

                        sh """
                        echo "Current PATH:"
                        echo \$PATH
                        echo "oc binary location:"
                        which oc
                        oc version
                        """

                        if (params.asChoice == "0") {

                            sh "touch ${env.REPLICAS_FILE}"

                            if (params.dryRun == "true") {
                                sh """
                                echo "=== DRY RUN: scale down ==="
                                echo "Replicas file: ${env.REPLICAS_FILE}"
                                echo "Project section [${project}] will be replaced with current deployment state:"
                                oc get deploy -o custom-columns=NAME:.metadata.name,REPLICAS:.spec.replicas --no-headers || true

                                echo "=== DRY RUN: scheduler/timeout deployments would be scaled to 0 ==="
                                oc get deploy -o custom-columns=NAME:.metadata.name --no-headers | grep -Ei "scheduler|timeout" || echo "no matches"

                                echo "=== DRY RUN: other deployments would be scaled to 0 ==="
                                oc get deploy -o custom-columns=NAME:.metadata.name --no-headers | grep -Ev "scheduler|timeout" || echo "no matches"
                                """
                            } else {
                                sh """
                                echo "Checking replicas state file before update"
                                ls -l ${env.REPLICAS_FILE} || echo "File not found"

                                if grep -q '\\[${project}\\]' ${env.REPLICAS_FILE}; then
                                    echo "Project section found, removing..."
                                    awk '/\\[${project}\\]/{flag=1;next}/\\[/{flag=0}flag{next}1' ${env.REPLICAS_FILE} > temp && mv temp ${env.REPLICAS_FILE}
                                else
                                    echo "No existing section found for project"
                                fi

                                echo "Adding section [${project}]"
                                echo '[${project}]' >> ${env.REPLICAS_FILE}
                                oc get deploy -o custom-columns=NAME:.metadata.name,REPLICAS:.spec.replicas --no-headers >> ${env.REPLICAS_FILE}
                                echo "" >> ${env.REPLICAS_FILE}

                                echo "=== Scaling down scheduler/timeout deployments ==="
                                for DEPLOY in \$(oc get deploy -o custom-columns=NAME:.metadata.name --no-headers | grep -Ei "scheduler|timeout" || true); do
                                    echo "Scaling \$DEPLOY to 0"
                                    oc scale deployment/\$DEPLOY --replicas=0
                                done

                                echo "Waiting 60 seconds..."
                                sleep 60

                                echo "=== Scaling down remaining deployments ==="
                                for DEPLOY in \$(oc get deploy -o custom-columns=NAME:.metadata.name --no-headers | grep -Ev "scheduler|timeout" || true); do
                                    echo "Scaling \$DEPLOY to 0"
                                    oc scale deployment/\$DEPLOY --replicas=0
                                done
                                """

                                archiveArtifacts artifacts: "${env.REPLICAS_FILE}", fingerprint: true
                            }
                        }

                        else if (params.asChoice == "1") {

                            if (params.dryRun == "true") {
                                sh """
                                echo "=== DRY RUN: scale up ==="
                                if grep -q '\\[${project}\\]' ${env.REPLICAS_FILE}; then
                                    echo "Restoration section [${project}] will be used:"
                                    awk '/\\[${project}\\]/{flag=1;next}/\\[/{flag=0}flag' ${env.REPLICAS_FILE}
                                else
                                    echo "No saved state found, defaulting to 1 replica for all deployments:"
                                    oc get deploy -o custom-columns=NAME:.metadata.name --no-headers | awk '{print \$1, 1}'
                                fi
                                """
                            } else {

                                DEPLOY_LIST=""

                                if grep -q '\\[${project}\\]' ${env.REPLICAS_FILE}; then
                                echo "Restoring saved replica state"
                                DEPLOY_LIST=\$(awk '/\\[${project}\\]/{flag=1;next}/\\[/{flag=0}flag' ${env.REPLICAS_FILE})
                                else
                                echo "No saved state found, scaling all deployments to 1"
                                DEPLOY_LIST=\$(oc get deploy -o custom-columns=NAME:.metadata.name --no-headers | awk '{print \$1, 1}')
                                fi

                                # Group 1: ingress/egress workloads
                                GROUP1=\$(echo "\$DEPLOY_LIST" | grep -E 'ingress|egress' | grep -Ev 'scheduler|timeout' || true)

                                # Group 2: dataspace/unimon workloads
                                GROUP2=\$(echo "\$DEPLOY_LIST" | grep -E 'dataspace|unimon' | grep -Ev 'scheduler|timeout' || true)

                                # Other workloads
                                OTHER=\$(echo "\$DEPLOY_LIST" | grep -Ev 'scheduler|timeout|ingress|egress|dataspace|unimon' || true)

                                scale_group() {
                                    GROUP="\$1"
                                    if [ -n "\$GROUP" ]; then
                                    echo "\$GROUP" | while read DEPLOY REPLICAS; do
                                    [ -z "\$DEPLOY" ] && continue
                                    [ -z "\$REPLICAS" ] && REPLICAS=1
                                    echo "Scaling \$DEPLOY to \$REPLICAS"
                                    oc scale deployment/\$DEPLOY --replicas=\$REPLICAS &
                                            done
                                    wait
                                    fi
                                }

                                scale_group "\$GROUP1"
                                scale_group "\$GROUP2"
                                scale_group "\$OTHER"
                            }
                        }

                        else {
                            error "Unknown parameter value: ${params.asChoice}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (fileExists(env.REPLICAS_FILE)) {
                    archiveArtifacts artifacts: "${env.REPLICAS_FILE}", onlyIfSuccessful: false
                }
            }
        }
    }
}