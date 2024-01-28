package com.abort;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class MainApplication {

    public static void main(String[] args) throws URISyntaxException, IOException {
        String planJiraId = args[0];
        String projectName = args[1];
        String filePath = args[2];
        String token = XrayUtils.getAuthToken();
        String planId = XrayUtils.getTestPlanID(planJiraId,token);
        String passedTC = ReadWriteFileUtil.readFromFile(filePath);
        HashMap<String,String> testCases = XrayUtils.getTestCasesForPlan(planId,token);
        if(Integer.parseInt(XrayUtils.totalTCCount) != passedTC.split(",").length){
            String abortPlanId = XrayUtils.createAbortTestPlan(testCases,projectName,token);
            XrayUtils.removeTCFromTestPlan(passedTC,abortPlanId.split(",")[0],testCases,token);
            System.out.println("Please use this test plan id in case FT is aborted: "+abortPlanId.split(",")[1]);
        }
    }

    /**
     * Function to create a new test plan from failed cases only.
     * @param planJiraId
     * @param projectName
     * @param failedNITMsFilePath
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void createTestPlanForFailedCases(String planJiraId, String projectName,String failedNITMsFilePath) throws URISyntaxException, IOException {
        String token = XrayUtils.getAuthToken();
        String planId = XrayUtils.getTestPlanID(planJiraId,token);
        String failedTC = ReadWriteFileUtil.readFromFile(failedNITMsFilePath);
        HashMap<String,String> allTestCases = XrayUtils.getTestCasesForPlan(planId,token);
        HashMap<String,String> testCases = MainApplication.removeAllKeysExpceptGiven(failedTC,allTestCases);
        String newTestPlan = XrayUtils.createAbortTestPlan(testCases,projectName,token);
        System.out.println("Test Plan for failed cases: "+newTestPlan.split(",")[1]);
    }

    public static HashMap<String,String> removeAllKeysExpceptGiven(String keys,HashMap<String,String> allTestCases){
        String[] keysToKeep = keys.split(",");
        allTestCases.keySet().retainAll(Arrays.stream(keysToKeep).collect(Collectors.toSet()));
        return allTestCases;
    }
}