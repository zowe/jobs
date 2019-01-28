/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */
package org.zowe.jobs.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.api.common.exceptions.NoZosmfResponseEntityException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.test.ZoweApiTest;
import org.zowe.api.common.utils.JsonUtils;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ResponseUtils.class, ZosmfJobsService.class, RequestBuilder.class, JsonUtils.class,
        ContentType.class })
public class ZosmfJobsServiceTest extends ZoweApiTest {

    private static final String BASE_URL = "https://dummy.com/zosmf/";

    @Mock
    ZosmfConnector zosmfConnector;

    ZosmfJobsService jobsService;

    @Before
    public void setUp() throws Exception {
        jobsService = new ZosmfJobsService();
        jobsService.zosmfconnector = zosmfConnector;
        when(zosmfConnector.getFullUrl(anyString())).thenAnswer(new org.mockito.stubbing.Answer<URI>() {
            @Override
            public URI answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return new URI(BASE_URL + (String) args[0]);
            }
        });

        when(zosmfConnector.getFullUrl(anyString(), anyString())).thenAnswer(new org.mockito.stubbing.Answer<URI>() {
            @Override
            public URI answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return new URI(BASE_URL + (String) args[0] + "?" + (String) args[1]);
            }
        });
    }

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

        HttpResponse response = mockJsonResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobsResponse.json"));
        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs?owner=%s&prefix=%s", owner, prefix));
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.getJobs(prefix, owner, status));

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

        HttpResponse response = mockJsonResponse(HttpStatus.SC_BAD_REQUEST, loadTestFile(responsePath));
        checkExceptionThrownForGetJobsAndVerifyCalls(prefix, owner, expectedException, response);
    }

    @Test
    public void get_jobs_with_no_response_entity_from_zosmf_should_throw_exception() throws Exception {

        String prefix = "PREFIX";
        String owner = "OWNER";
        String path = String.format(BASE_URL + "restjobs/jobs?owner=%s&prefix=%s", owner, prefix);
        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.I_AM_A_TEAPOT;

        Exception expectedException = new NoZosmfResponseEntityException(status, path);

        HttpResponse response = mockResponse(status.value());
        checkExceptionThrownForGetJobsAndVerifyCalls(prefix, owner, expectedException, response);
    }

    private void checkExceptionThrownForGetJobsAndVerifyCalls(String prefix, String owner, Exception expectedException,
            HttpResponse response) throws IOException, Exception {

        String path = String.format("restjobs/jobs?owner=%s&prefix=%s", owner, prefix);
        RequestBuilder requestBuilder = mockGetBuilder(path);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.getJobs(prefix, owner, JobStatus.ALL));

        verifyInteractions(requestBuilder, true);
    }

    @Test
    public void get_job_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "AJOB";
        String jobId = "Job12345";

        Job expected = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT,
                "Job is on the hard copy queue", "CANCELED");

        HttpResponse response = mockJsonResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobResponse.json"));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.getJob(jobName, jobId));

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

        shouldThrow(expectedException, () -> jobsService.getJob(jobName, jobId));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_files_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";
        JobFile jesmsglg = JobFile.builder().id(2l).ddname("JESMSGLG").recfm("UA").lrecl(133l).byteCount(1103l)
            .recordCount(20l).build();
        JobFile jesjcl = JobFile.builder().id(3l).ddname("JESJCL").recfm("V").lrecl(136l).byteCount(182l)
            .recordCount(3l).build();
        JobFile jesysmsg = JobFile.builder().id(4l).ddname("JESYSMSG").recfm("VA").lrecl(137l).byteCount(820l)
            .recordCount(13l).build();
        List<JobFile> expected = Arrays.asList(jesmsglg, jesjcl, jesysmsg);

        HttpResponse response = mockJsonResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobFilesResponse.json"));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s/files", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.getJobFiles(jobName, jobId));

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
        HttpResponse response = mockJsonResponse(statusCode, loadTestFile(file));

        RequestBuilder requestBuilder = mockGetBuilder(String.format("restjobs/jobs/%s/%s/files", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.getJobFiles(jobName, jobId));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_file_content_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21489";
        String fileId = "3";
        JobFileContent expected = new JobFileContent(
                "        1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\n"
                        + "          //*        TEST JOB\n        2 //UNIT     EXEC PGM=IEFBR14\n" + "");

        HttpResponse response = mockTextResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobFileRecordsResponse.txt"));

        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.getJobFileContent(jobName, jobId, fileId));

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
        HttpResponse response = mockJsonResponse(statusCode, loadTestFile(file));

        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.getJobFileContent(jobName, jobId, fileId));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_jcl_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21489";
        JobFileContent expected = new JobFileContent(
                "        1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\n"
                        + "          //*        TEST JOB\n        2 //UNIT     EXEC PGM=IEFBR14\n" + "");

        HttpResponse response = mockTextResponse(HttpStatus.SC_OK, loadTestFile("zosmf_getJobFileRecordsResponse.txt"));

        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, "3"));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.getJobJcl(jobName, jobId));

        verifyInteractions(requestBuilder);
    }

    @Test
    public void get_job_jcl_for_non_existing_jobname_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        checkGetJobJclExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_getJob_noJobNameResponse.json");
    }

    @Test
    public void get_job_jcl_for_non_existing_job_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "z000000";

        Exception expectedException = new JobIdNotFoundException(jobName, jobId);

        checkGetJobJclExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "zosmf_getJobFiles_noJobIdResponse.json");
    }

    @Test
    public void get_job_jcl_for_non_existing_field_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21849";

        Exception expectedException = new JobJesjclNotFoundException(jobName, jobId);

        checkGetJobJclExceptionAndVerify(jobName, jobId, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_getJobFileRecords_noJesJcl.json");
    }

    private void checkGetJobJclExceptionAndVerify(String jobName, String jobId, Exception expectedException,
            int statusCode, String file) throws IOException, Exception {
        HttpResponse response = mockJsonResponse(statusCode, loadTestFile(file));

        RequestBuilder requestBuilder = mockGetBuilder(
                String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, "3"));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.getJobJcl(jobName, jobId));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void submit_job_string_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jclString = "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440\n" + "//*        TEST JOB\n"
                + "//UNIT     EXEC PGM=IEFBR14\n";

        Job expected = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT,
                "Job is on the hard copy queue", "CANCELED");

        HttpResponse response = mockJsonResponse(HttpStatus.SC_CREATED, loadTestFile("zosmf_getJobResponse.json"));

        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", jclString);

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.submitJobString(jclString));

        verifyInteractions(requestBuilder);
        verify(requestBuilder).addHeader("Content-type", ContentType.TEXT_PLAIN.getMimeType());
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Class", "A");
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Recfm", "F");
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Lrecl", "80");
        verify(requestBuilder).addHeader("X-IBM-Intrdr-Mode", "TEXT");
    }

    @Test
    public void submit_job_string_with_no_slash_should_call_zosmf_parse_and_throw_exception() throws Exception {
        Exception expectedException = new ZoweApiRestException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Submit input data does not start with a slash");
        checkExceptionThrownForSubmitJclStringAndVerifyCalls("junkJCL\n", "zosmf_submitJcl_noSlash.json",
                expectedException);
    }

    @Test
    public void submit_job_string_with_bad_jcl_should_call_zosmf_parse_and_throw_exception() throws Exception {
        Exception expectedException = new ZoweApiRestException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Job input was not recognized by system as a job");
        checkExceptionThrownForSubmitJclStringAndVerifyCalls("//But still junkJCL\n", "zosmf_submitJcl_invalid.json",
                expectedException);
    }

    @Test
    public void submit_job_string_with_too_long_jcl_should_call_zosmf_parse_and_throw_exception() throws Exception {
        Exception expectedException = new ZoweApiRestException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Job submission error. Record length 103 too long for JCL submission, maxlen=80");
        checkExceptionThrownForSubmitJclStringAndVerifyCalls(
                "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440//*        TEST JOB//UNIT     EXEC PGM=IEFBR14",
                "zosmf_submitJcl_tooLong.json", expectedException);
    }

    private void checkExceptionThrownForSubmitJclStringAndVerifyCalls(String badJcl, String responsePath,
            Exception expectedException) throws IOException, Exception {

        HttpResponse response = mockJsonResponse(HttpStatus.SC_BAD_REQUEST, loadTestFile(responsePath));
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", badJcl);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.submitJobString(badJcl));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void submit_job_data_set_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String dataSet = "STEVENH.TEST.JCL(IEFBR14)";

        Job expected = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT,
                "Job is on the hard copy queue", "CANCELED");

        HttpResponse response = mockJsonResponse(HttpStatus.SC_CREATED, loadTestFile("zosmf_getJobResponse.json"));

        // TODO MAYBE map zosmf model object?
        JsonObject body = new JsonObject();
        body.addProperty("file", "//'" + dataSet + "'");
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", body);

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, jobsService.submitJobFile(dataSet));

        verifyInteractions(requestBuilder);
    }

    @Test
    public void submit_job_data_set_with_no_member_call_zosmf_parse_and_throw_exception() throws Exception {
        String dataSet = "STEVENH.TEST.JCL(INVALID)";

        Exception expectedException = new DataSetNotFoundException(dataSet);

        HttpResponse response = mockJsonResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                loadTestFile("zosmf_submitJobByFile_noDatasetMember.json"));

        JsonObject body = new JsonObject();
        body.addProperty("file", "//'" + dataSet + "'");
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", body);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.submitJobFile(dataSet));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void submit_job_data_set_with_no_data_set_call_zosmf_parse_and_throw_exception() throws Exception {
        String dataSet = "INVALID.TEST.JCL(IEFBR14)";

        // TODO create exception for this?
        Exception expectedException = new DataSetNotFoundException(dataSet);

        HttpResponse response = mockJsonResponse(HttpStatus.SC_BAD_REQUEST,
                loadTestFile("zosmf_submitJobByFile_noDataset.json"));

        JsonObject body = new JsonObject();
        body.addProperty("file", "//'" + dataSet + "'");
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", body);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.submitJobFile(dataSet));
        verifyInteractions(requestBuilder);
    }

    @Test
    public void purge_job_string_should_call_zosmf_correctly() throws Exception {
        String jobName = "AJOB";
        String jobId = "Job12345";

        HttpResponse response = mockResponse(HttpStatus.SC_ACCEPTED);

        RequestBuilder requestBuilder = mockDeleteBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        jobsService.purgeJob(jobName, jobId);

        verifyInteractions(requestBuilder);
    }

    @Test
    public void purge_job_for_non_existing_job_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        HttpResponse response = mockJsonResponse(HttpStatus.SC_BAD_REQUEST,
                loadTestFile("zosmf_getJob_noJobNameResponse.json"));

        RequestBuilder requestBuilder = mockDeleteBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.purgeJob(jobName, jobId));
        verifyInteractions(requestBuilder);
    }

    private static Job createJob(String id, String jobName, String owner, String type, JobStatus status, String phase,
            String returnCode) {
        return Job.builder().jobId(id) // $NON-NLS-1$
            .jobName(jobName) // $NON-NLS-1$
            .owner(owner) // $NON-NLS-1$
            .type(type) // $NON-NLS-1$
            .status(status) // $NON-NLS-1$
            .subsystem("JES2") //$NON-NLS-1$
            .executionClass(type) // $NON-NLS-1$
            .phaseName(phase) // $NON-NLS-1$
            .returnCode(returnCode) // $NON-NLS-1$
            .build();
    }

    // TODO - refactor with datasets
    private void verifyInteractions(RequestBuilder requestBuilder) throws IOException, URISyntaxException {
        verifyInteractions(requestBuilder, false);
    }

    // TODO - improve code - remove bool?
    private void verifyInteractions(RequestBuilder requestBuilder, boolean path)
            throws IOException, URISyntaxException {
        verify(zosmfConnector, times(1)).request(requestBuilder);
        if (path) {
            verify(zosmfConnector, times(1)).getFullUrl(anyString(), anyString());
        } else {
            verify(zosmfConnector, times(1)).getFullUrl(anyString());
        }
        verifyNoMoreInteractions(zosmfConnector);
    }

    private RequestBuilder mockGetBuilder(String relativeUri) throws URISyntaxException {
        RequestBuilder builder = mock(RequestBuilder.class);
        mockStatic(RequestBuilder.class);
        when(RequestBuilder.get(new URI(BASE_URL + relativeUri))).thenReturn(builder);
        return builder;
    }

    private RequestBuilder mockDeleteBuilder(String relativeUri) throws URISyntaxException {
        RequestBuilder builder = mock(RequestBuilder.class);
        mockStatic(RequestBuilder.class);
        when(RequestBuilder.delete(new URI(BASE_URL + relativeUri))).thenReturn(builder);
        return builder;
    }

    private RequestBuilder mockPutBuilder(String relativeUri, String string) throws Exception {
        StringEntity stringEntity = mock(StringEntity.class);
        PowerMockito.whenNew(StringEntity.class).withArguments(string).thenReturn(stringEntity);
        return mockPutBuilder(relativeUri, stringEntity);
    }

    private RequestBuilder mockPutBuilder(String relativeUri, JsonObject json) throws Exception {
        StringEntity stringEntity = mock(StringEntity.class);
        PowerMockito.whenNew(StringEntity.class).withArguments(json.toString(), ContentType.APPLICATION_JSON)
            .thenReturn(stringEntity);

        return mockPutBuilder(relativeUri, stringEntity);
    }

    private RequestBuilder mockPutBuilder(String relativeUri, StringEntity stringEntity) throws Exception {
        RequestBuilder builder = mock(RequestBuilder.class);

        mockStatic(RequestBuilder.class);
        when(RequestBuilder.put(new URI(BASE_URL + relativeUri))).thenReturn(builder);
        when(builder.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")).thenReturn(builder);
        when(builder.setEntity(stringEntity)).thenReturn(builder);
        return builder;
    }

    private RequestBuilder mockPostBuilder(String relativeUri, String string) throws Exception {
        StringEntity stringEntity = mock(StringEntity.class);
        PowerMockito.whenNew(StringEntity.class).withArguments(string).thenReturn(stringEntity);
        return mockPostBuilder(relativeUri, stringEntity);
    }

    private RequestBuilder mockPostBuilder(String relativeUri, JsonObject json) throws Exception {
        StringEntity stringEntity = mock(StringEntity.class);
        PowerMockito.whenNew(StringEntity.class).withArguments(json.toString(), ContentType.APPLICATION_JSON)
            .thenReturn(stringEntity);

        return mockPostBuilder(relativeUri, stringEntity);
    }

    private RequestBuilder mockPostBuilder(String relativeUri, StringEntity stringEntity) throws Exception {
        RequestBuilder builder = mock(RequestBuilder.class);

        mockStatic(RequestBuilder.class);
        when(RequestBuilder.post(new URI(BASE_URL + relativeUri))).thenReturn(builder);
        when(builder.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")).thenReturn(builder);
        when(builder.setEntity(stringEntity)).thenReturn(builder);
        return builder;
    }

    private HttpResponse mockJsonResponse(int statusCode, String jsonString) throws IOException {

        HttpEntity entity = new StringEntity(jsonString);
        HttpResponse response = mockResponse(statusCode);
        when(response.getEntity()).thenReturn(entity);

        JsonElement json = new Gson().fromJson(jsonString, JsonElement.class);
        when(ResponseUtils.getEntityAsJson(response)).thenReturn(json);

        ContentType contentType = mock(ContentType.class);
        mockStatic(ContentType.class);
        when(ContentType.get(entity)).thenReturn(contentType);
        when(contentType.getMimeType()).thenReturn(ContentType.APPLICATION_JSON.getMimeType());

        if (json.isJsonArray()) {
            when(ResponseUtils.getEntityAsJsonArray(response)).thenReturn(json.getAsJsonArray());
        } else if (json.isJsonObject()) {
            when(ResponseUtils.getEntityAsJsonObject(response)).thenReturn(json.getAsJsonObject());
        }

        return response;
    }

    private HttpResponse mockTextResponse(int statusCode, String text) throws IOException {

        HttpEntity entity = new StringEntity(text);
        HttpResponse response = mockResponse(statusCode);
        when(response.getEntity()).thenReturn(entity);

        when(ResponseUtils.getEntity(response)).thenReturn(text);

        ContentType contentType = mock(ContentType.class);
        mockStatic(ContentType.class);
        when(ContentType.get(entity)).thenReturn(contentType);
        when(contentType.getMimeType()).thenReturn(ContentType.TEXT_PLAIN.getMimeType());

        return response;
    }

    private HttpResponse mockResponse(int statusCode) throws IOException {
        HttpResponse response = mock(HttpResponse.class);
        mockStatic(ResponseUtils.class);
        when(ResponseUtils.getStatus(response)).thenReturn(statusCode);
        return response;
    }

    public String loadTestFile(String relativePath) throws IOException {
        return loadFile("src/test/resources/zosmfResponses/" + relativePath);
    }
}
