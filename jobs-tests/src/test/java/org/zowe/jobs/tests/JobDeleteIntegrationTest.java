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

import io.restassured.http.ContentType;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.SimpleJob;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;

public class JobDeleteIntegrationTest extends AbstractJobsIntegrationTest {

    @Test
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
    
    @Test
    public void testDeleteJobs() throws Exception {
        Job job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        Job job2 = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        ArrayList<SimpleJob> jobsList = new ArrayList<SimpleJob>();
        jobsList.add(new SimpleJob(job.getJobName(), job.getJobId()));
        jobsList.add(new SimpleJob(job2.getJobName(), job2.getJobId()));
        
        deleteJobs(jobsList).then().statusCode(HttpStatus.SC_NO_CONTENT).body(equalTo(""));
    }
    
    @Test
    public void testDeleteJobsOneInvalidJob() throws Exception {
        Job job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        SimpleJob job2 = new SimpleJob("DUMMYJOB", "JOB00000");
        ArrayList<SimpleJob> jobsList = new ArrayList<SimpleJob>();
        jobsList.add(new SimpleJob(job.getJobName(), job.getJobId()));
        jobsList.add(new SimpleJob(job2.getJobName(), job2.getJobId()));
        ApiError expectedError = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
            .message(String.format("No job with name '%s' and id '%s' was found", job2.getJobName(), job2.getJobId()))
            .build();
        deleteJobs(jobsList).then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
            .body("status", equalTo(expectedError.getStatus().name()))
            .body("message", equalTo(expectedError.getMessage()));
    }
}