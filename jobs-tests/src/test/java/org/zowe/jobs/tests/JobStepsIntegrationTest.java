/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2020
 */

package org.zowe.jobs.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.JobStep;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JobStepsIntegrationTest extends AbstractJobsIntegrationTest {

    private static Job job;

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
    }

    @AfterClass
    public static void purgeJob() throws Exception {
        deleteJob(job);
    }

    @Test
    public void testGetJobSteps() throws Exception {
        JobStep step = JobStep.builder().name("UNIT").program("IEFBR14").step(1).build();
        List<JobStep> expected = Collections.singletonList(step);
        List<JobStep> actual = getJobSteps(job.getJobName(), job.getJobId()).then().statusCode(HttpStatus.SC_OK)
            .extract().body().jsonPath().getList("", JobStep.class);

        assertEquals(expected, actual);
    }

    // TODO test job with no steps?

    @Test
    public void testGetJobOutputStepsInvalidJobId() throws Exception {
        String jobName = job.getJobName();
        String jobId = "z0000000";
        verifyExceptionReturn(new JobNameNotFoundException(jobName, jobId), getJobSteps(jobName, jobId));
    }

    @Test
    public void testGetJobOutputStepsInvalidJobNameAndId() throws Exception {
        String jobName = "z";
        String jobId = "z0000000";
        verifyExceptionReturn(new JobNameNotFoundException(jobName, jobId), getJobSteps(jobName, jobId));
    }

    public static Response getJobSteps(String jobName, String jobId) throws Exception {
        return RestAssured.given().header(AUTH_HEADER).when().get(getJobPath(jobName, jobId) + "/steps");
    }
}