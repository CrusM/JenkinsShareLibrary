// jira触发事件
package org.devops

def HttpReq(reqType, reqUrl, reqBody) {
    // 发送github api请求
    // reqType：请求方法 GET,POST,PUT
    // reqUrl： 接口请求url
    // reqBody： 参数内容
    def jiraServer = 'http://my.jira.com/rest/api/2'
    result = httpRequest(acceptType: 'APPLICATION_JSON_UTF8', 
                        authentication: '72c1989c-f40a-4ffa-8304-2f99e1757ddf',
                        contentType: 'APPLICATION_JSON_UTF8', 
                        httpMode: "${reqType}",
                        responseHandle: 'NONE',
                        consoleLogResponseBody: true,
                        ignoreSslErrors: true, 
                        url: "${jiraServer}/${reqUrl}")
    return result
}

// 执行JQL语句
def RunJql(jqlContent) {
    apiUrl = "search?jql=${jqlContent}"
    respone = HttpReq("GET", apiUrl, "")
    return respone
}
