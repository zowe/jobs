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
import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.zowe.api.common.exceptions.HtmlEscapedZoweApiRestException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({ SubmitJobStringZosmfRequestRunner.class })
public class SubmitJobStringZosmfRequestRunnerTest extends AbstractZosmfJobsRequestRunnerTest {

    @Test
    public void submit_job_string_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jclString = "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440\n" + "//*        TEST JOB\n"
                + "//UNIT     EXEC PGM=IEFBR14\n";

        Job expected = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT,
                "Job is on the hard copy queue", "CANCELED");

        mockJsonResponse(HttpStatus.SC_CREATED, loadTestFile("zosmf_getJobResponse.json"));

        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", jclString);

        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);

        assertEquals(expected, new SubmitJobStringZosmfRequestRunner(jclString).run(zosmfConnector));

        verifyInteractions(requestBuilder);
        verify(requestBuilder).addHeader("Content-type", ContentType.TEXT_PLAIN.getMimeType());
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Class", "A");
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Recfm", "F");
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Lrecl", "80");
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Mode", "TEXT");
    }

    @Test
    public void submit_job_string_with_no_slash_should_call_zosmf_parse_and_throw_exception() throws Exception {
        Exception expectedException = new HtmlEscapedZoweApiRestException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Submit input data does not start with a slash");
        checkExceptionThrownForSubmitJclStringAndVerifyCalls("junkJCL\n", "zosmf_submitJcl_noSlash.json",
                expectedException);
    }

    @Test
    public void submit_job_string_with_bad_jcl_should_call_zosmf_parse_and_throw_exception() throws Exception {
        Exception expectedException = new HtmlEscapedZoweApiRestException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Job input was not recognized by system as a job");
        checkExceptionThrownForSubmitJclStringAndVerifyCalls("//But still junkJCL\n", "zosmf_submitJcl_invalid.json",
                expectedException);
    }

    @Test
    public void submit_job_string_with_too_long_jcl_should_call_zosmf_parse_and_throw_exception() throws Exception {
        Exception expectedException = new HtmlEscapedZoweApiRestException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Job submission error. Record length 103 too long for JCL submission, maxlen=80");
        checkExceptionThrownForSubmitJclStringAndVerifyCalls(
                "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440//*        TEST JOB//UNIT     EXEC PGM=IEFBR14",
                "zosmf_submitJcl_tooLong.json", expectedException);
    }

    private void checkExceptionThrownForSubmitJclStringAndVerifyCalls(String badJcl, String responsePath,
            Exception expectedException) throws IOException, Exception {

        mockJsonResponse(HttpStatus.SC_BAD_REQUEST, loadTestFile(responsePath));
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", badJcl);
        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> new SubmitJobStringZosmfRequestRunner(badJcl).run(zosmfConnector));
        verifyInteractions(requestBuilder);
    }

}
