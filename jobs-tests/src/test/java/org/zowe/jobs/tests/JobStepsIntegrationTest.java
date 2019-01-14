/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2019
 */

package org.zowe.jobs.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.JobStep;
import org.zowe.tests.IntegrationTestResponse;

import java.util.Collections;
import java.util.List;

//TODO - rewrite using rest assured
public class JobStepsIntegrationTest extends AbstractJobsIntegrationTest {

    private static Job job;

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
    }

    @AfterClass
    public static void purgeJob() throws Exception {
        purgeJob(job);
    }

    @Test
    public void testGetJobSteps() throws Exception {
        JobStep step = JobStep.builder().name("UNIT").program("IEFBR14").step(1).build();
        List<JobStep> expected = Collections.singletonList(step);
        getJobSteps(job.getJobName(), job.getJobId()).shouldHaveStatusOk().shouldHaveEntity(expected);
    }

    // TODO test job with no steps?

    @Test
    public void testGetJobOutputStepsInvalidJobId() throws Exception {
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), "z000000"))
                .build();

        getJobSteps(job.getJobName(), "z000000").shouldReturnError(expected);
    }

    @Test
    public void testGetJobOutputStepsInvalidJobNameAndId() throws Exception {
        // TODO MAYBE - use exception?
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message(String.format("No job with name '%s' and id '%s' was found", "z", "z000000")).build();

        getJobSteps("z", "z000000").shouldReturnError(expected);
    }

    public static IntegrationTestResponse getJobSteps(String jobName, String jobId) throws Exception {
        return sendGetRequest2(getJobUri(jobName, jobId) + "/steps");
    }
}