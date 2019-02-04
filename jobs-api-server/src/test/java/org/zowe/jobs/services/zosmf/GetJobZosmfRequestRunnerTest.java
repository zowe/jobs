package org.zowe.jobs.services.zosmf;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GetJobZosmfRequestRunnerTest extends AbstractZosmfJobsRequestRunnerTest {

    @Test
    public void get_job_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "AJOB";
        String jobId = "Job12345";

        Job expected = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT,
                "Job is on the hard copy queue", "CANCELED");

        HttpResponse response = mockJsonResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobResponse.json"));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        GetJobZosmfRequestRunner runner = new GetJobZosmfRequestRunner(jobName, jobId);
        assertEquals(expected, runner.run(zosmfConnector));

        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_for_non_existing_jobname_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        checkGetJobExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_getJob_noJobNameResponse.json");
    }

    @Test
    public void get_job_for_non_existing_jobid_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOBhjh4";

        Exception expectedException = new JobIdNotFoundException(jobName, jobId);

        checkGetJobExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "zosmf_getJob_noJobIdResponse.json");
    }

    private void checkGetJobExceptionAndVerify(String jobName, String jobId, Exception expectedException,
            int statusCode, String file) throws IOException, Exception {
        HttpResponse response = mockJsonResponse(statusCode, loadTestFile(file));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        GetJobZosmfRequestRunner runner = new GetJobZosmfRequestRunner(jobName, jobId);
        shouldThrow(expectedException, () -> runner.run(zosmfConnector));
        verifyInteractions(requestBuilder);
    }

}
