/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */
package org.zowe.jobs.services.zosmf;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.zowe.api.common.exceptions.NoZosmfResponseEntityException;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GetJobsZosmfRequestRunnerTest extends AbstractZosmfJobsRequestRunnerTest {

    @Test
    public void get_all_status_jobs_should_call_zosmf_and_parse_response_correctly() throws Exception {
        Job job1 = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT, "Job is on the hard copy queue",
                "CANCELED");
        Job job2 = createJob("STC16821", "ZOWESVR", "IZUSVR", "STC", JobStatus.ACTIVE, "Job is actively executing",
                null);
        Job job3 = createJob("TSU06806", "ZOWESH", "STEVENH", "TSU", JobStatus.INPUT, "Job is queued for execution",
                null);
        Job job4 = createJob("TSU14480", "ZOWESH", "STEVENH", "TSU", JobStatus.OUTPUT, "Job is on the hard copy queue",
                "ABEND S222");

        test_getJobs(JobStatus.ALL, Arrays.asList(job1, job2, job3, job4));
    }

    @Test
    public void get_output_jobs_should_call_zosmf_and_parse_response_correctly() throws Exception {
        Job job1 = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT, "Job is on the hard copy queue",
                "CANCELED");
        Job job4 = createJob("TSU14480", "ZOWESH", "STEVENH", "TSU", JobStatus.OUTPUT, "Job is on the hard copy queue",
                "ABEND S222");

        test_getJobs(JobStatus.OUTPUT, Arrays.asList(job1, job4));
    }

    private void test_getJobs(JobStatus status, List<Job> expected) throws Exception {
        String owner = "*";
        String prefix = "ZO*";

        mockJsonResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobsResponse.json"));
        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs?owner=%s&prefix=%s", owner, prefix));
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        GetJobsZosmfRequestRunner runner = new GetJobsZosmfRequestRunner(prefix, owner, status);
        assertEquals(expected, runner.run(zosmfConnector));

        verifyInteractions(requestBuilder, true);
    }

    @Test
    public void get_jobs_with_invalid_prefix_should_call_zosmf_parse_and_throw_exception() throws Exception {

        String prefix = "123456798";
        String owner = "*";
        testInvalidGetJob(prefix, owner, "zosmf_getJobs_invalid_prefixResponse.json",
                new InvalidPrefixException(prefix));
    }

    @Test
    public void get_jobs_with_invalid_owner_should_call_zosmf_parse_and_throw_exception() throws Exception {

        String prefix = "*";
        String owner = "123456789";
        testInvalidGetJob(prefix, owner, "zosmf_getJobs_invalid_ownerResponse.json", new InvalidOwnerException(owner));
    }

    private void testInvalidGetJob(String prefix, String owner, String responsePath, Exception expectedException)
            throws Exception {

        mockJsonResponse(HttpStatus.SC_BAD_REQUEST, loadTestFile(responsePath));
        checkExceptionThrownForGetJobsAndVerifyCalls(prefix, owner, expectedException, response);
    }

    @Test
    public void get_jobs_with_no_response_entity_from_zosmf_should_throw_exception() throws Exception {

        String prefix = "PREFIX";
        String owner = "OWNER";
        String path = String.format(BASE_URL + "restjobs/jobs?owner=%s&prefix=%s", owner, prefix);
        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.I_AM_A_TEAPOT;

        Exception expectedException = new NoZosmfResponseEntityException(status, path);

        mockResponseCache(status.value());
        checkExceptionThrownForGetJobsAndVerifyCalls(prefix, owner, expectedException, response);
    }

    private void checkExceptionThrownForGetJobsAndVerifyCalls(String prefix, String owner, Exception expectedException,
            HttpResponse response) throws IOException, Exception {

        String path = String.format("restjobs/jobs?owner=%s&prefix=%s", owner, prefix);
        RequestBuilder requestBuilder = mockGetBuilder(path);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        GetJobsZosmfRequestRunner runner = new GetJobsZosmfRequestRunner(prefix, owner, JobStatus.ALL);
        shouldThrow(expectedException, () -> runner.run(zosmfConnector));

        verifyInteractions(requestBuilder, true);
    }

}
