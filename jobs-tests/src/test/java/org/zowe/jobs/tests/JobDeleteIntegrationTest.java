/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package org.zowe.jobs.tests;

import io.restassured.http.ContentType;

import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import static org.hamcrest.CoreMatchers.equalTo;

public class JobDeleteIntegrationTest extends AbstractJobsIntegrationTest {

    @Test
    @Ignore("See todo") // TODO - fix CIM server on river/change to pre-req JES2
    public void testDeleteJob() throws Exception {
        Job job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        deleteJob(job).then().statusCode(HttpStatus.SC_NO_CONTENT).body(equalTo(""));
    }

    @Test
    public void testDeleteJobInvalidJob() throws Exception {
        Job job = Job.builder().jobName("DUMMYJOB").jobId("JOB00000").build();
        ApiError expectedError = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
            .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), job.getJobId()))
            .build();
        deleteJob(job).then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
            .body("status", equalTo(expectedError.getStatus().name()))
            .body("message", equalTo(expectedError.getMessage()));
    }
}