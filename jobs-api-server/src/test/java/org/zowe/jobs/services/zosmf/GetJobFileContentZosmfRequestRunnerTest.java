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
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.JobFileContent;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GetJobFileContentZosmfRequestRunnerTest extends AbstractZosmfRequestRunnerTest {

    @Test
    public void get_job_file_content_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21489";
        String fileId = "3";

        JobFileContent expected = new JobFileContent(
                "        1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\n"
                        + "          //*        TEST JOB\n        2 //UNIT     EXEC PGM=IEFBR14\n" + "");

        mockTextResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobFileRecordsResponse.txt"));

        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        GetJobFileContentZosmfRequestRunner runner = new GetJobFileContentZosmfRequestRunner(jobName, jobId, fileId);

        assertEquals(expected, runner.run(zosmfConnector));

        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_files_content_for_non_existing_jobname_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";
        String fileId = "3";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        checkGetJobFileContentExceptionAndVerify(jobName, jobId, fileId, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_getJob_noJobNameResponse.json");
    }

    @Test
    public void get_job_files_content_for_non_existing_job_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "z000000";
        String fileId = "3";

        Exception expectedException = new JobIdNotFoundException(jobName, jobId);

        checkGetJobFileContentExceptionAndVerify(jobName, jobId, fileId, expectedException,
                HttpStatus.SC_INTERNAL_SERVER_ERROR, "zosmf_getJobFiles_noJobIdResponse.json");
    }

    @Test
    public void get_job_files_content_for_non_existing_field_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21849";
        String fileId = "1";

        Exception expectedException = new JobFileIdNotFoundException(jobName, jobId, fileId);

        checkGetJobFileContentExceptionAndVerify(jobName, jobId, fileId, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_getJobFileRecords_invalidFileId.json");
    }

    private void checkGetJobFileContentExceptionAndVerify(String jobName, String jobId, String fileId,
            Exception expectedException, int statusCode, String file) throws IOException, Exception {
        mockJsonResponse(statusCode, loadTestFile(file));

        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        GetJobFileContentZosmfRequestRunner runner = new GetJobFileContentZosmfRequestRunner(jobName, jobId, fileId);
        shouldThrow(expectedException, () -> runner.run(zosmfConnector));
        verifyInteractions(requestBuilder);
    }

}
