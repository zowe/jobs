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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.api.common.test.services.zosmf.AbstractZosmfRequestRunnerTest;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.services.zosmf.GetJobFilesZosmfRequestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GetJobFilesZosmfRequestRunnerTest extends AbstractZosmfRequestRunnerTest {

    @Test
    public void get_job_files_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";
        JobFile jesmsglg = JobFile.builder().id(2l).ddName("JESMSGLG").recordFormat("UA").recordLength(133l)
            .byteCount(1103l).recordCount(20l).build();
        JobFile jesjcl = JobFile.builder().id(3l).ddName("JESJCL").recordFormat("V").recordLength(136l).byteCount(182l)
            .recordCount(3l).build();
        JobFile jesysmsg = JobFile.builder().id(4l).ddName("JESYSMSG").recordFormat("VA").recordLength(137l)
            .byteCount(820l).recordCount(13l).build();
        List<JobFile> jobFiles = Arrays.asList(jesmsglg, jesjcl, jesysmsg);
        ItemsWrapper<JobFile> expected = new ItemsWrapper<JobFile>(jobFiles);

        mockJsonResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobFilesResponse.json"));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s/files", jobName, jobId));

        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);

        GetJobFilesZosmfRequestRunner runner = new GetJobFilesZosmfRequestRunner(jobName, jobId);
        assertEquals(expected, runner.run(zosmfConnector));

        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_files_for_non_existing_jobname_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        checkGetJobFilesExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_getJob_noJobNameResponse.json");
    }

    @Test
    public void get_job_files_for_non_existing_job_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "z000000";

        Exception expectedException = new JobIdNotFoundException(jobName, jobId);

        checkGetJobFilesExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "zosmf_getJobFiles_noJobIdResponse.json");
    }

    private void checkGetJobFilesExceptionAndVerify(String jobName, String jobId, Exception expectedException,
            int statusCode, String file) throws IOException, Exception {
        mockJsonResponse(statusCode, loadTestFile(file));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s/files", jobName, jobId));

        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);

        GetJobFilesZosmfRequestRunner runner = new GetJobFilesZosmfRequestRunner(jobName, jobId);
        shouldThrow(expectedException, () -> runner.run(zosmfConnector));
        verifyInteractions(requestBuilder);
    }
}
