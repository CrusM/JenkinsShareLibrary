#!groovy
// tapd相关操作

package org.devops

// 定义网络请求方法

def HttpReq(reqType, reqUrl, reqBody) {
    // 发送api请求
    // reqType：请求方法 GET,POST,PUT
    // reqUrl： 接口请求url
    // reqBody： 参数内容
    def serverUrl = 'https://api.tapd.cn'
    result = httpRequest(acceptType: 'APPLICATION_JSON_UTF8', 
                        contentType: 'APPLICATION_JSON_UTF8',
                        authentication: 'a116dbb4-4839-4531-9e76-e1fc6920bbb0',
                        httpMode: "${reqType}",
                        responseHandle: 'NONE',
                        requestBody: reqBody,
                        consoleLogResponseBody: true,
                        ignoreSslErrors: true, 
                        url: "${serverUrl}/${reqUrl}")
    return result
}

// 整合url封装
def UrlEncode(apiUrl, Map paramsMap = [:]) {
    if (paramsMap.size() > 0) {
        apiUrl += "?"
        paramsMap.each {
            apiUrl+= "&" +  it.key + "=" + it.value
        }
    }
    return apiUrl
}

// 整合获取信息api
def GetInfoApi(apiName, Map paramsMap = [:]) {
    apiUrl = UrlEncode(apiName, paramsMap)
    response = HttpReq("GET", apiUrl, "")
    return response
}

// 获取需求信息
def GetStoryInfo(workspace_id, Map paramsMap = [:]) {
    apiUrl = "stories?workspace_id=${workspace_id}"
    paramsMap.each{entry -> apiUrl+= "&" +  entry.key + "=" + entry.value}
    response = HttpReq("GET", apiUrl, "")
    return response
}

// 获取需求计数
def GetStoryCount(workspace_id, Map paramsMap = [:]) {
    apiUrl = "stories/count?workspace_id=${workspace_id}"
    paramsMap.each{entry -> apiUrl+= "&" +  entry.key + "=" + entry.value}
    response = HttpReq("GET", apiUrl, '')
    res = readJSON text: """${response.content}"""
    return res["data"]["count"]
}

// 获取缺陷(BUG)信息
def GetBugInfo(workspace_id, Map paramsMap = [:]) {
    apiUrl = "bugs?workspace_id=${workspace_id}"
    paramsMap.each{entry -> apiUrl +="&" + entry.key + "=" + entry.value}
    response = HttpReq("GET", apiUrl, "")
    return response
}

// 获取需求计数
def GetBugCount(workspace_id, Map paramsMap = [:]) {
    apiUrl = "bugs/count?workspace_id=${workspace_id}"
    paramsMap.each{entry -> apiUrl +="&" + entry.key + "=" + entry.value}
    response = HttpReq("GET", apiUrl, '')
    res = readJSON text: """${response.content}"""
    return res["data"]["count"]
}

// 获取迭代关联的需求
def GetIterationStories(workspace_id, iterId) {
    def storyCount = GetStoryCount(workspace_id, ["iteration_id": iterId])
    int count = 0
    int page = 1
    def modules = []
    def paramsMap = []
    if(storyCount>0) {
        while(count<storyCount) {
            response = GetInfoApi("stories", ["workspace_id":workspace_id,"iteration_id": iterId, "page": page])
            res = readJSON text: """${response.content}"""
            for(storyInfo in res["data"]) {
                status = storyInfo["Story"]["status"]
                if(status!="resolved") {
                    println("${storyInfo.Story.name} 未实现")
                    return false
                }
                modules.add(["id":storyInfo["Story"]["id"],"moduleName":storyInfo["Story"]["module"]])
            }
            count += 30
            page += 1
        }
    }
    return modules
}

// 获取迭代关联的缺陷
def GetIterationBugs(workspace_id, iterId) {
    def bugCount = GetBugCount(workspace_id, ["iteration_id": iterId])
    def modules = []
    count = 0
    page = 1
    while(count<bugCount) {
        response = GetBugInfo(workspace_id, ["iteration_id": iterId, "page": page])
        res = readJSON text: """${response.content}"""
        for(bugInfo in res["data"]) {
            status = bugInfo["Bug"]["status"]

            if(status!="verified" && status!="closed") {
                println("${bugInfo.Bug.title} 未关闭")
                return false
            }

            modules.add(["id":bugInfo["Bug"]["id"],"moduleName":bugInfo["Bug"]["module"]])
        }
        count += 30
        page += 1
    }
    return modules
}
// 获取迭代关联的modules
def GetIterationModules(workspace_id, iterId) {

    // 获取当前迭代下的所有需求
    def modules = []
    storiesModules = GetIterationStories(workspace_id, iterId)
    if(storiesModules)  {
        storiesModules.each{ 
            modules.add(it.moduleName) 
            }
    }
    // 获取当前迭代下的所有缺陷
    bugsModules = GetIterationBugs(workspace_id, iterId)
    if(bugsModules) {
        bugsModules.each{
            modules.add(it.moduleName) 
        }
    }

    return modules.unique()
}
