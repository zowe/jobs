/*
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
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.text.IsEqualIgnoringCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JobsGetIntegrationTest extends AbstractJobsIntegrationTest {

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
    public void testGetJobs() {
        List<Job> actual = getJobs(null, null).then().statusCode(HttpStatus.SC_OK).extract().body().jsonPath()
            .getList("", Job.class);

        // We have results, they are of type job and all have owner = user
        assertTrue(actual.size() > 0);
        for (Job job : actual) {
            assertThat(job.getOwner(), IsEqualIgnoringCase.equalToIgnoringCase(USER));
        }
    }

    @Test
    public void testGetJobsWithUnlikelyPrefix() {
        String prefix = "12345678";
        getJobs(prefix, null).then().statusCode(HttpStatus.SC_OK).body("$", IsEmptyCollection.empty());
    }

    @Test
    public void testGetJobsWithInvalidPrefix() throws Exception {
        String prefix = "123456789";
        ZoweApiRestException expected = new InvalidPrefixException(prefix);

        ApiError expectedError = expected.getApiError();

        getJobs(prefix, null).then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
            .body("status", equalTo(expectedError.getStatus().name()))
            .body("message", equalTo(expectedError.getMessage()));
    }

    @Test
    public void testGetJobsWithUnlikelyOwner() {
        String owner = "12345678";
        getJobs(null, owner).then().statusCode(HttpStatus.SC_OK).body("$", IsEmptyCollection.empty());
    }

    @Test
    public void testGetJobsWithInvalidOwner() throws Exception {
        String owner = "123456789";
        ZoweApiRestException expected = new InvalidOwnerException(owner);

        ApiError expectedError = expected.getApiError();

        getJobs(null, owner).then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
            .body("status", equalTo(expectedError.getStatus().name()))
            .body("message", equalTo(expectedError.getMessage()));
    }

    @Test
    public void testGetJobsWithHtmlOwner() throws Exception {
        String owner = "*s23y3%3cscript%3ealert(1)%3c%2fscript%3evpgqn";

        ZoweApiRestException expected = new InvalidOwnerException(owner);

        ApiError expectedError = expected.getApiError();

        getJobs(null, owner).then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
            .body("status", equalTo(expectedError.getStatus().name())).body("message",
                    equalTo("An invalid job owner of '*s23y3&lt;script&gt;alert(1)&lt;/script&gt;vpgqn' was supplied"));
    }

    @Test
    public void testGetJobsWithOwnerAndPrefix() {
        String owner = USER;
        String prefix = job.getJobName();
        List<Job> actual = getJobs(prefix, owner).then().statusCode(HttpStatus.SC_OK).extract().body().jsonPath()
            .getList("", Job.class);

        // We have results, they are of type job and all have owner = user and prefix
        assertTrue(actual.size() > 0);
        for (Job job : actual) {
            assertThat(job.getOwner(), IsEqualIgnoringCase.equalToIgnoringCase(USER));
            assertThat(job.getJobName(), org.hamcrest.core.StringStartsWith.startsWith(prefix));
        }
    }

    @Test
    public void testGetJobsWithCurrentUserAsOwnerAndSpecificPrefix() throws Exception {
        String prefix = job.getJobName();
        List<Job> actual = getJobs(prefix, null).then().statusCode(HttpStatus.SC_OK).extract().body().jsonPath()
            .getList("", Job.class);

        // We have results, they are of type job and all have owner = user and prefix
        assertTrue(actual.size() > 0);
        for (Job job : actual) {
            assertThat(job.getOwner(), IsEqualIgnoringCase.equalToIgnoringCase(USER));
            assertThat(job.getJobName(), org.hamcrest.core.StringStartsWith.startsWith(prefix));
        }
    }

    @Test
    public void testGetJobsWithCurrentUserAsOwnerSpecificPrefixAndStatus() throws Exception {
        List<Job> actual = getJobs(null, null, JobStatus.OUTPUT).then().statusCode(HttpStatus.SC_OK).extract().body()
            .jsonPath().getList("", Job.class);

        // We have results, they are of type job and all have owner = user and status OUTPUT
        assertTrue(actual.size() > 0);
        for (Job job : actual) {
            assertThat(job.getOwner(), IsEqualIgnoringCase.equalToIgnoringCase(USER));
            assertEquals(JobStatus.OUTPUT, job.getStatus());
        }
    }
}