package org.devops

def HttpReq(reqType, reqUrl, reqBody) {
    // 发送github api请求
    // reqType：请求方法 GET,POST,PUT
    // reqUrl： 接口请求url
    // reqBody： 参数内容
    def gitServer = 'http://192.168.0.38/api/v4'
    withCredentials(
        [string(
            credentialsId: '05c27b41-e5bf-4dee-a874-88e8d78aa950', 
            variable: 'gitlabToken'
        )]) {
            result = httpRequest(acceptType: 'APPLICATION_JSON_UTF8', 
                                contentType: 'APPLICATION_JSON_UTF8', 
                                customHeaders: [
                                    [
                                        maskValue: true, 
                                        name: 'PRIVATE-TOKEN', 
                                        value: "${gitlabToken}"
                                    ]
                                ], 
                                httpMode: "${reqType}",
                                responseHandle: 'NONE',
                                requestBody: reqBody,
                                consoleLogResponseBody: true,
                                ignoreSslErrors: true, 
                                url: "${gitServer}/${reqUrl}")
    }
    return result
}

// 变更commit状态
def ChangeCommitStatus(projectId, commitSha, status, description = ""){
    commitApi = "projects/${projectId}/statuses/${commitSha}?state=${status}&description=${description}"
    response = HttpReq("POST", commitApi,'')
    return response
}

// 获取projectId
def GetProjectId(projectName, group = ''){
    apiUrl = "projects?search=${projectName}"
    response = HttpReq("GET", apiUrl,'')
    def result =  readJSON text: """${response.content}"""
    println("result: ${result}")
    for (repo in result) {
        if(repo['path'] == "${projectName}") {
            repoId = repo["id"]
            if(group && group == "${repo.namespace.path}") {
                return repoId
            } else if(group == '') {
                return repoId
            }
        }
    }
}

// 删除分支
def DeleteProjectBranch(projectId, branchName) {
    apiUrl = "projects/${projectId}/repository/branches/${branchName}"
    response = HttpReq('DELETE', apiUrl, '').content
    println(response)
}

// 创建分支
def CreateProjectBranch(projectId,refBranch, branchName) {
    try{
        apiUrl = "projects/${projectId}/repository/branches?branch=${branchName}&ref=${refBranch}"
        response = HttpReq("POST", apiUrl,'').content
        branchInfo  = readJSON text: """${response}"""
        return branchInfo
    } catch(e) {
        println(e)
    }
}

// 创建合并请求
def CreateProjectMR(projectId, sourceBranch, targetBranch, title, description="", assigneeUser='') {
    try{
        def apiUrl = "projects/${projectId}/merge_requests"
        def reqBody = """{"source_branch":"${sourceBranch}","target_branch": "${targetBranch}", "title": "${title}","description":"${description}","assignee_id": "${assigneeUser}"}"""
        response = HttpReq("POST", apiUrl, reqBody)
        return response
    } catch(e) {
        println("创建合并请求 -- ${projectId} -- ${sourceBranch} --> ${targetBranch} 异常，e")
    }
}

// 搜索分支
def SearchProjectBranches(projectId, searchKey) {
    def apiUrl = "projects/${projectId}/repository/branches?search=${searchKey}"
    response = HttpReq("GET", apiUrl, "").content
    def branchInfo = readJSON text: """${response}"""
    def branches = [:]
    branches[projectId] = []
    if(branchInfo.size()==0) {
        return branches
    } else {
        for (branch in branchInfo) {
            branches[projectId] += [
                "branchName": branch["name"],
                "commitMes" : branch["commit"]["message"],
                "commitId" : branch["commit"]["id"],
                "merged" : branch["merged"],
                "createTime" : branch["commit"]["created_at"]
            ]
        }
        return branches
    }
}

// 新建tag
def CreateProjectTag(projectId, tag_name, ref = 'master') {
    // projectId : gitlab project id
    // tag_name :  
    // ref    ：  source branch
    try{
        def apiUrl = "/projects/${projectId}/repository/tags"
        def reqBody = """{"tag_name": "${tag_name}", "ref": "${ref}"}"""
        response = HttpReq("POST", apiUrl, reqBody).content
        return response
    } catch(e) {
        println(e)
    }
}

// 获取commit信息
def GetCommitSha(projectId, branch) {

    if(branch.contains('/')) {
        branch = branch.split('/')[1]
    }

    apiUrl = "projects/${projectId}/repository/commits?ref_name=${branch}"
    response = HttpReq("GET", apiUrl, "").content
    response = readJSON text: """${response}"""
    return response[0].short_id
}

// 获取合并

// 确认合并请求
def AcceptMr(projectId, mergeId){
    def apiUrl = "projects/${projectId}/merge_requests/${mergeId}/merge"
    HttpReq('PUT',apiUrl,'')
}
