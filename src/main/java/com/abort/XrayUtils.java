package com.abort;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;
import static io.restassured.RestAssured.expect;

public class XrayUtils {

    static HashMap<String, String> testCases;
    public static String totalTCCount;
    private final static String XrayClientId = "your-xray-client-id",
            XrayClientSecretKey = "your-xray-client-secret-key";
    private final static String baseUrl = "your-jira-server-url";
    public static String getAuthToken(){
        String bodydata = "",accessToken = "";
        Map<String, Object> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("client_id", XrayClientId);
        map.put("client_secret", XrayClientSecretKey);
        ObjectMapper mapper = new ObjectMapper();
        try {
            bodydata = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        accessToken = expect().statusCode(200).and().statusLine("HTTP/1.1 200 OK").given().headers(header).with()
                .body(bodydata).relaxedHTTPSValidation().when()
                .post(baseUrl+"/api/v1/authenticate").getHeader("x-access-token");
        return accessToken;
    }

    public static String  getTestPlanID(String planName,String token){
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + token);
        String url = baseUrl+"/api/v1/graphql";
        String data = "{\r\n" + "    \"query\": \"{\\n  getTestPlans(jql: \\\"key = " + planName.replace("_1_retry", "").replace("_2_retry", "")
                + "\\\", start: " + 0 + ", limit: " + 100
                + ") \\n  {\\n    total\\n    results {\\n      issueId,      \\n      tests(limit: 100) {\\n          total,\\n          results {\\n            issueId\\n            testType {\\n              name\\n            }\\n            jira(fields: [\\\"key\\\",\\\"assignee\\\", \\\"reporter\\\"]) \\n          }\\n        }\\n      jira(fields: [\\\"key\\\",\\\"assignee\\\", \\\"reporter\\\"]) \\n    }\\n  }\\n}\\n\",\r\n"
                + "    \"variables\": {}\r\n" + "}";

        String responseBody = expect().statusCode(200).and().statusLine("HTTP/1.1 200 OK").given().headers(headers).with()
                .body(data).relaxedHTTPSValidation().when()
                .post(url).getBody()
                .asString();
        JSONObject json = new JSONObject(responseBody);
        String planId = json.getJSONObject("data").getJSONObject("getTestPlans").getJSONArray("results").getJSONObject(0).getString("issueId").toString();
        return planId;
    }

    public static HashMap<String,String> getTestCasesForPlan(String planId,String token) throws MalformedURLException, URISyntaxException, IOException {
        int start = 0, limit = 100;
        testCases = new HashMap<String,String>();
        String url = baseUrl+"/api/v1/graphql";
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + token);
        do {
            String data = "{\"query\":\"{\\n    getTestPlan(issueId: \\\"" + planId + "\\\") {\\n        issueId\\n        projectId\\n    \\t\\ttests(limit: " + limit + ", start: " + start + ") {\\n          total,\\n          results {\\n            issueId\\n            testType {\\n              name\\n            }\\n            jira(fields: [\\\"key\\\",\\\"assignee\\\", \\\"reporter\\\"]) \\n          }\\n        }\\n        jira(fields: [\\\"assignee\\\", \\\"reporter\\\"])\\n    }\\n}\",\"variables\":{}}";
            String responseBody = expect().statusCode(200).and().statusLine("HTTP/1.1 200 OK").given().headers(headers).with()
                    .body(data).relaxedHTTPSValidation().when()
                    .post(url).getBody()
                    .asString();
            JSONObject json = new JSONObject(responseBody);
            totalTCCount = json.getJSONObject("data").getJSONObject("getTestPlan").getJSONObject("tests").get("total").toString();
            JSONArray results = json.getJSONObject("data").getJSONObject("getTestPlan").getJSONObject("tests").getJSONArray("results");
            for (Object result : results) {
                try {
                    JSONObject res = new JSONObject(result.toString());
                    testCases.put(res.getJSONObject("jira").get("key").toString(),res.get("issueId").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            start += limit;
        }
        while (start < Integer.parseInt(totalTCCount));
        return testCases;
    }

    public static String createAbortTestPlan(HashMap<String,String> mapOfCases,String project, String token) throws IOException, URISyntaxException {
        String listOfCases = String.join(",", mapOfCases.values()).replace(",", "\\\", \\\"");
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " +token);
        String data = "{\"query\":\"mutation {\\n    createTestPlan(\\n        testIssueIds: [\\\"" + listOfCases +"\\\"]\\n        jira: {\\n            fields: {\\n                summary: \\\"Retry Test Plan\\\",\\n                project: {key: \\\"" + project + "\\\"} \\n            }\\n        }\\n    ) {\\n        testPlan {\\n            issueId\\n            jira(fields: [\\\"key\\\"])\\n        }\\n        warnings\\n    }\\n}\",\"variables\":{}}";
        String url = baseUrl+"/api/v1/graphql";
        String responseBody = expect().statusCode(200).and().statusLine("HTTP/1.1 200 OK").given().headers(headers).with()
                .body(data).relaxedHTTPSValidation().when()
                .post(url).getBody()
                .asString();
        JSONObject json = new JSONObject(responseBody);
        String planName = json.getJSONObject("data").getJSONObject("createTestPlan").getJSONObject("testPlan").getJSONObject("jira").getString("key");
        String planId = json.getJSONObject("data").getJSONObject("createTestPlan").getJSONObject("testPlan").getString("issueId");
        return planId+","+planName;
    }

    public static void removeTCFromTestPlan(String tcJira,String planId,HashMap<String,String> testCases,String token) throws URISyntaxException, IOException {
        String[] jiraIds = tcJira.split(",");
        List<String> ids = new ArrayList<String>();
        for(String id:jiraIds)
            ids.add(testCases.get(id).toString());
        String idsForTCs = String.join(",", ids).replace(",", "\\\", \\\"");
        String data = "{\"query\":\"mutation {\\n    removeTestsFromTestPlan(\\n        issueId: \\\"" + planId + "\\\",\\n        testIssueIds: [\\\""+idsForTCs+"\\\"]\\n    ) \\n}\",\"variables\":{}}";
        String url = baseUrl+"/api/v1/graphql";
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " +token);
        expect().statusCode(200).and().statusLine("HTTP/1.1 200 OK").given().headers(headers).with()
                .body(data).relaxedHTTPSValidation().when()
                .post(url).getBody()
                .asString();
    }

}
