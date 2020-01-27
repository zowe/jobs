/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019, 2020
 */

package org.zowe.jobs.tests;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import com.google.gson.JsonObject;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.List;

public class JobModifyIntegrationTest extends AbstractJobsIntegrationTest {
    
    @Before
    public void prepareSystemForTest() throws Exception {
        //Ensure there are no existing LONGJOB jobs on the system
        List<Job> jobs = getJobs("LONGJOB", "*").then().statusCode(HttpStatus.SC_OK).extract().body().jsonPath()
                .getList("items", Job.class);
        for (Job job : jobs) {
            deleteJob(job).then().log().all().statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }
    
    @Test
    public void testCancelJob() throws Exception {
        Job job = submitJobAndPoll(LONGJOB, JobStatus.ACTIVE);
        modifyJob(job, "cancel").then().statusCode(HttpStatus.SC_ACCEPTED).body(equalTo(""));
        for (int i = 0; i < 20; i++) {
            Response response = getJob(job.getJobName(), job.getJobId());
            if (response.then().extract().statusCode() == HttpStatus.SC_OK) {
                Job jobResponse = response.then().extract().body().as(Job.class);
                if (jobResponse.getStatus() == JobStatus.OUTPUT) {
                    break;
                }
            }
            Thread.sleep(1200); //NOSONAR
        }
    }
    
    @Test
    public void testModifyJobInvalidJob() throws Exception {
        Job job = Job.builder().jobName("DUMMYJOB").jobId("JOB12345").build();
        ApiError expectedError = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message((String.format("No job with name '%s' and id '%s' was found", job.getJobName(), job.getJobId()))).build();
        modifyJob(job, "cancel").then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
        .body("status", equalTo(expectedError.getStatus().name()))
        .body("message", equalTo(expectedError.getMessage()));
    }
    
    @Test
    public void testModifyJobInvalidModifyRequest() throws Exception {
        Job job = submitJobAndPoll(LONGJOB, JobStatus.ACTIVE);
        ApiError expectedError = ApiError.builder().status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .message("Update request is not &#39;cancel, hold or release&#39;").build();
        modifyJob(job, "burn").then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
        .body("status", equalTo(expectedError.getStatus().name()))
        .body("message", equalTo(expectedError.getMessage()));
        
        deleteJob(job).then().statusCode(HttpStatus.SC_NO_CONTENT);
    }
    
    public static Response modifyJob(Job job, String command) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("command", command);
        Response response = RestAssured.given().contentType("application/json").body(body.toString()).when().put(getJobPath(job));
        return response;
    }
}
