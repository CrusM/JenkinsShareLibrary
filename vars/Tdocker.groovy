#!groovy
// 应用发布到docker环境 pipline 模板
def call(map){
    pipeline {
        agent any
        tools {
            maven 'mvn3.6'
            jdk 'jdk8'
            gradle 'gradle4'
        }

        options {
            // 同一个pipeline，禁止同时执行多次
            disableConcurrentBuilds()
            // 保留历史构建数量
            buildDiscarder(logRotator(numToKeepStr: '3'))
        }

        // 参数话构建
        parameters {
            gitParameter(branch: '',
                        branchFilter: "${map.branchFilter}",
                        defaultValue: 'develop',
                        description: '',
                        name: 'BRANCH',
                        quickFilterEnabled: false,
                        selectedValue: 'NONE',
                        sortMode: 'NONE',
                        tagFilter: '*',
                        type: 'PT_BRANCH_TAG',
                        useRepository: "${map.gitUrl}")
            
            string(name: "PORT", defaultValue: "${map.PORT}", description: "发布的端口")
            string(name: "active", defaultValue: "${map.active}", description: "程序启动环境")
            string(name: "MAXMEM", defaultValue: "${map.MAXMEM}", description: "jvm最大可分配内存")
            string(name: "APPID", defaultValue: "${map.APPID}", description: "apollo appid")
            string(name: "playbook", defaultValue: "${map.playbook}", description: "ansible playbook路径(运维负责)")
        }
        // 定义环境变量
        environment{
            __REGISTRY_URL = "registry.cn-hangzhou.aliyuncs.com/pailian/app"
            __version = ZgetVersion("${map.gitUrl}", "${params.BRANCH}")
            __tool = "${map.tool}"
            __customImage = "${__REGISTRY_URL}" +":${map.build_name}" + "-${__version}"
            __workspace = "${map.workspace}"
            DOCKER_BUILDKIT = '1'
        }

        //
        stages {
        // 检出代码
            stage('checkout') {
                steps {
                    checkout([$class: 'GitSCM',
                            branches: [[name: "${branch}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [],
                            gitTool: 'Default',
                            submoduleCfg: [],
                            userRemoteConfigs: [[url: "${gitUrl}",credentialsId: "${token}"]]
                    ])
                }
            }

        // 入库

            stage('push&&sonar') {
                parallel {
                    // 构建镜像
                    stage("构建镜像") {
                        // build镜像
                        steps {
                            withDockerRegistry([
                                    credentialsId: "${DOCKER_REGISTRY_TOKEN_ID}",
                                    url: "https://" + "${REGISTRY_URL}"
                                ]) {
                                    sh "docker build -t ${__customImage} -f ${WORKSPACE}/Dockerfile ."
                                    sh "docker push ${__customImage}"
                                }
                    
                            }
                    }
                    
                    stage("代码扫描") {
                        steps {
                            dir("${__workspace}") {
                                script {
                                    sonarqubeScannerHome = tool name: 'sonarqubeScanner'
                                    out = sh(script:"ls sonar-project.properties",returnStatus:true)
                                }
                                if(out==0) {
                                    withSonarQubeEnv("${sonarqubeName}") {
                                        sh "${sonarqubeScannerHome}/bin/sonar-scanner -Dsonar.host.url=${SONAR_HOST_URL} \
                                            -Dsonar.login=${SONAR_AUTH_TOKEN}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        
        // 部署
            stage('Deploy') {
                steps {
                    ansiblePlaybook(
                        become: true, 
                        credentialsId: '8f980c24-3a0f-418b-b9c1-1b6d3e95b599',
                        installation: 'ansible', 
                        // inventory: '/var/jenkins_home/workspace/ops-ansible/hosts', 
                        playbook: playbook_path,
                        extraVars: [
                            project: project,
                            image: "${__customImage}",
                            hosts: hosts,
                            PORT: PORT,
                            MAXMEM: MAXMEM,
                            active: active,
                            APPID: APPID
                            ]
                    )
                }
            }
        }
    }
}
