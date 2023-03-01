package org.devops


//scan

def SonarScanJava(projectName,projectDesc,projectPath, sonarServer = "sonarqube"){
    
    //定义服务器列表
    println("扫描java程序")
    script {
            sonarqubeScannerHome = tool name: 'sonarqubeScanner'
        }
    println("开始扫描java程序")
    withSonarQubeEnv("sonarqube") {
            sh "${sonarqubeScannerHome}/bin/sonar-scanner -Dsonar.host.url=${SONAR_HOST_URL} \
                -Dsonar.login=${SONAR_AUTH_TOKEN} \
                -Dsonar.projectKey=${projectName} \
                -Dsonar.projectName=${projectName} \
                -Dsonar.projectVersion=${commit_sha} \
                -Dsonar.ws.timeout=30 \
                -Dsonar.projectDescription=${projectDesc} \
                -Dsonar.sources=${projectPath} \
                -Dsonar.sourceEncoding=UTF-8 \
                -Dsonar.java.binaries=target/classes \
                -Dsonar.java.source=1.8 \
                -Dsonar.java.libraries=${JENKINS_HOME}/.m2 \
                -Dsonar.java.test.binaries=target/test-classes \
                -Dsonar.java.surefire.report=target/surefire-reports \
                -Dsonar.branch.name=${branch} \
                -Dsonar.language=java \
                -Dsonar.analsis.mode=preview \
                -Dsonar.gitlab.project_id=${projectId} \
                -Dsonar.gitlab.commit_sha=${commit_sha} \
                -Dsonar.gitlab.ref_name=${branch}"
            }
    
    //def qg = waitForQualityGate()
    //if (qg.status != 'OK') {
        //error "Pipeline aborted due to quality gate failure: ${qg.status}"
    //}
}

def SonarScanHTML(projectName,projectDesc,projectPath, sonarServer = "sonarqube"){
    
    //定义服务器列表
    script {
            sonarqubeScannerHome = tool name: 'sonarqubeScanner'
        }
    
    
    withSonarQubeEnv("sonarqube") {
        sh "${sonarqubeScannerHome}/bin/sonar-scanner -Dsonar.host.url=${SONAR_HOST_URL} \
            -Dsonar.login=${SONAR_AUTH_TOKEN} \
                -Dsonar.projectKey=${projectName} \
                -Dsonar.projectName=${projectName} \
                -Dsonar.projectVersion=${commit_sha} \
                -Dsonar.ws.timeout=30 \
                -Dsonar.projectDescription=${projectDesc} \
                -Dsonar.sources=${projectPath} \
                -Dsonar.sourceEncoding=UTF-8 \
                -Dsonar.branch.name=${branch} \
                -Dsonar.analsis.mode=preview \
                -Dsonar.gitlab.project_id=${projectId} \
                -Dsonar.gitlab.commit_sha=${commit_sha} \
                -Dsonar.gitlab.ref_name=${branch}"
    }

    //def qg = waitForQualityGate()
    //if (qg.status != 'OK') {
        //error "Pipeline aborted due to quality gate failure: ${qg.status}"
    //}
}
