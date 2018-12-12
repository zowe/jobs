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
    public static void setUpJobDatasetsIfRequired() throws Exception {
        // TODO - fix AbstractDatasetsIntegrationTest.initialiseDatasetsIfNescessary();

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

    // TODO - work out better solution?
//    static String getTestJclMemberPath(String member) {
//        return USER.toUpperCase() + ".TEST.JCL(" + member + ")";
//    }

    public static Job submitJobString(String jobFile) throws Exception {
        return submitJobJclStringFromFile(jobFile).shouldHaveStatusCreated().getEntityAs(Job.class);
    }

    static IntegrationTestResponse submitJobJclStringFromFile(String jobFile) throws Exception {
        String jcl = new String(Files.readAllBytes(Paths.get("testFiles/" + jobFile)));
        return submitJobJclString(jcl);
    }

    static IntegrationTestResponse submitJobJclString(String jclString) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("jcl", jclString);
        return sendPostRequest(JOBS_ROOT_ENDPOINT, body);
    }

//    public static Job submitJobFile(String jobFile) throws Exception {
//        String jobFileString = "'" + getTestJclMemberPath(jobFile) + "'";
//        return submitJobByFile(jobFileString).shouldHaveStatusCreated().getEntityAs(Job.class);
//    }

    public static IntegrationTestResponse purgeJob(Job job) throws Exception {
        return sendDeleteRequest(getJobUri(job));
    }

    public static IntegrationTestResponse getJob(Job job) throws Exception {
        return sendGetRequest2(getJobUri(job));
    }

    public static IntegrationTestResponse getJob(String jobName, String jobId) throws Exception {
        return sendGetRequest2(getJobUri(jobName, jobId));
    }

    private static String getJobUri(String jobName, String jobId) {
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
            IntegrationTestResponse response = getJob(jobName, jobId);
            System.out.println("Response status is: " + response.getStatus());
            if (response.getStatus() == HttpStatus.SC_OK) {
                if (waitForState != null) {
                    Job jobResponse = response.getEntityAs(Job.class);
                    System.out.println("Job status is: " + jobResponse.getStatus());
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

        return sendPostRequest(JOBS_ROOT_ENDPOINT, body);
    }
}