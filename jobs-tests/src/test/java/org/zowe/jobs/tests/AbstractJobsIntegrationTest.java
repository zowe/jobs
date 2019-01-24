/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */
package org.zowe.jobs.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.zowe.api.common.utils.JsonUtils;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.tests.AbstractHttpComparisonTest;
import org.zowe.tests.IntegrationTestResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class AbstractJobsIntegrationTest extends AbstractHttpComparisonTest {

    static final String JOBS_ROOT_ENDPOINT = "jobs";

    static final String JOB_IEFBR14 = "IEFBR14";
    static final String JOB_WITH_STEPS = "JOB1DD";

    static final String TEST_JCL_PDS = USER.toUpperCase() + ".TEST.JCL";

    @BeforeClass
    public static void setUpEndpoint() throws Exception {
        RestAssured.basePath = JOBS_ROOT_ENDPOINT;
    }

    static Job submitJobAndPoll(String testJobName) throws Exception {
        return submitJobAndPoll(testJobName, null);
    }

    static Job submitJobAndPoll(String testJobName, JobStatus waitForState) throws Exception {

        Job job = submitJobString(testJobName);
        String jobName = job.getJobName();
        String jobId = job.getJobId();
        assertPoll(jobName, jobId, waitForState);
        return job;
    }

    public static Job submitJobString(String jobFile) throws Exception {
        return submitJobJclStringFromFile(jobFile).then().statusCode(HttpStatus.SC_CREATED).extract().body()
            .as(Job.class);
    }

    static Response submitJobJclStringFromFile(String jobFile) throws Exception {
        String jcl = new String(Files.readAllBytes(Paths.get("testFiles/" + jobFile)));
        return submitJobJclString(jcl);
    }

    static Response submitJobJclString(String jclString) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("jcl", jclString);
        return RestAssured.given().contentType("application/json").body(body.toString()).when().post("/string");
    }

    public static Response deleteJob(Job job) throws Exception {
        return RestAssured.given().when().delete(getJobPath(job));
    }

    static Response getJobs(String prefix, String owner) {
        return getJobs(prefix, owner, null);
    }

    static Response getJobs(String prefix, String owner, JobStatus status) {
        RequestSpecification request = RestAssured.given();
        if (prefix != null) {
            request = request.queryParam("prefix", prefix);
        }
        if (owner != null) {
            request = request.queryParam("owner", owner);
        }
        if (status != null) {
            request = request.queryParam("status", status.name());
        }
        return request.when().get();
    }

    public static IntegrationTestResponse getJob(Job job) throws Exception {
        return sendGetRequest2(getJobUri(job));
    }

    public static IntegrationTestResponse getJobA(String jobName, String jobId) throws Exception {
        return sendGetRequest2(getJobUri(jobName, jobId));
    }

    static Response getJob(String jobName, String jobId) throws Exception {
        return RestAssured.given().when().get(jobName + "/" + jobId);
    }

    protected static String getJobPath(Job job) {
        return job.getJobName() + "/" + job.getJobId();
    }

    protected static String getJobUri(String jobName, String jobId) {
        return JOBS_ROOT_ENDPOINT + "/" + jobName + "/" + jobId;
    }

    private static String getJobUri(Job job) {
        return getJobUri(job.getJobName(), job.getJobId());
    }

    private static void assertPoll(String jobName, String jobId, JobStatus waitForState) throws Exception {
        Assert.assertTrue("Failed to verify job submit, jobname:" + jobName + ", jobid:" + jobId,
                pollJob(jobName, jobId, waitForState));
    }

    public static boolean pollJob(String jobName, String jobId, JobStatus waitForState) throws Exception {
        for (int i = 0; i < 20; i++) {
            Response response = getJob(jobName, jobId);
            if (response.then().extract().statusCode() == HttpStatus.SC_OK) {
                if (waitForState != null) {
                    Job jobResponse = response.then().extract().body().as(Job.class);
                    if (waitForState == jobResponse.getStatus()) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            Thread.sleep(1200);
        }
        return false;
    }

    static HashMap<String, String> getSubstitutionVars(Job job) {
        HashMap<String, String> substitutionVars = new HashMap<>();
        substitutionVars.put("JOBNAME", job.getJobName());
        substitutionVars.put("JOBID", job.getJobId());
        substitutionVars.put("ATLAS.USERNAME", USER);
        return substitutionVars;
    }

    Job getSubstitutionVars(Job toUpdate, Job updateJob) {
        if ("${JOBID}".equals(toUpdate.getJobId())) {
            toUpdate.setJobId(updateJob.getJobId());
        }
        if ("${JOBNAME}".equals(toUpdate.getJobName())) {
            toUpdate.setJobName(updateJob.getJobName());
        }
        if (JobStatus.ALL == toUpdate.getStatus()) {
            toUpdate.setStatus(updateJob.getStatus());
        }
        if ("${ANY}".equals(toUpdate.getPhaseName())) {
            toUpdate.setPhaseName(updateJob.getPhaseName());
        }
        if ("${ANY}".equals(toUpdate.getReturnCode())) {
            toUpdate.setReturnCode(updateJob.getReturnCode());
        }
        if ("${ANY}".equals(toUpdate.getExecutionClass())) {
            toUpdate.setExecutionClass(updateJob.getExecutionClass());
        }
        if ("${ATLAS.USERNAME}".equals(toUpdate.getOwner())) {
            toUpdate.setOwner(USER.toUpperCase());
        }
        return toUpdate;
    }

    void verifyJobIsAsExpected(String expectedResultFilePath, Job actualJob)
            throws JsonParseException, JsonMappingException, IOException {
        Job expectedJob = JsonUtils.convertFilePath(Paths.get(expectedResultFilePath), Job.class);
        expectedJob = getSubstitutionVars(expectedJob, actualJob);

        assertEquals(expectedJob, actualJob);
    }

    static IntegrationTestResponse submitJobByFile(String fileString) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("file", fileString);

        return sendPostRequest(JOBS_ROOT_ENDPOINT + "/dataset", body);
    }
}