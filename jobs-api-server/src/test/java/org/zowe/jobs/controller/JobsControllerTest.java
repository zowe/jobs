/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */
package org.zowe.jobs.controller;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.api.common.errors.ApiError;
import org.zowe.api.common.exceptions.ZoweApiErrorException;
import org.zowe.api.common.exceptions.ZoweRestExceptionHandler;
import org.zowe.api.common.test.ZoweApiTest;
import org.zowe.api.common.utils.JsonUtils;
import org.zowe.api.common.utils.ZosUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.SubmitJobFileRequest;
import org.zowe.jobs.model.SubmitJobStringRequest;
import org.zowe.jobs.services.JobsService;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ZosUtils.class, ServletUriComponentsBuilder.class })
public class JobsControllerTest extends ZoweApiTest {

    private static final String DUMMY_USER = "A_USER";

    private MockMvc mockMvc;

    @Mock
    private JobsService jobsService;

    @InjectMocks
    private JobsController jobsController;

    // TODO LATER - move up into ApiControllerTest?
    @Before
    public void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobsController).setControllerAdvice(new ZoweRestExceptionHandler())
                .build();

        PowerMockito.mockStatic(ZosUtils.class);
        when(ZosUtils.getUsername()).thenReturn(DUMMY_USER);
    }

    @Test
    public void test_get_jobs_with_owner_and_prefix_works() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);

        when(jobsService.getJobs("TESTNAME", "*", JobStatus.ALL)).thenReturn(jobs);

        mockMvc.perform(get("/api/v1/jobs?prefix={prefix}&owner={owner}", "TESTNAME", "*")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(jobs)));

        verify(jobsService, times(1)).getJobs("TESTNAME", "*", JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_jobs_with_owner_and_prefix_and_status_works() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);

        when(jobsService.getJobs("TESTNAME", "*", JobStatus.ACTIVE)).thenReturn(jobs);

        mockMvc.perform(
                get("/api/v1/jobs?prefix={prefix}&owner={owner}&status={status}", "TESTNAME", "*", JobStatus.ACTIVE))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(jobs)));

        verify(jobsService, times(1)).getJobs("TESTNAME", "*", JobStatus.ACTIVE);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_jobs_with_no_owner_defaults() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);

        when(jobsService.getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL)).thenReturn(jobs);

        mockMvc.perform(get("/api/v1/jobs?prefix={prefix}", "TESTNAME")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(jobs)));

        verify(jobsService, times(1)).getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_jobs_with_no_results_returns_empty_body() throws Exception {

        List<Job> jobs = Collections.emptyList();

        when(jobsService.getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL)).thenReturn(jobs);

        mockMvc.perform(get("/api/v1/jobs?prefix={prefix}", "TESTNAME")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("[]"));

        verify(jobsService, times(1)).getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void get_jobs_with_exception_should_be_converted_to_error_message() throws Exception {

        String invalidOwner = "DUMMY_OWNER";

        String errorMessage = MessageFormat.format("An invalid job owner of ''{0}'' was supplied", invalidOwner);
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.BAD_REQUEST).build();

        InvalidOwnerException zoweException = new InvalidOwnerException(invalidOwner);
        when(jobsService.getJobs("TESTNAME", invalidOwner, JobStatus.ALL)).thenThrow(zoweException);

        mockMvc.perform(get("/api/v1/jobs?prefix={prefix}&owner={owner}", "TESTNAME", invalidOwner))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJobs("TESTNAME", invalidOwner, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_job_with_jobId_and_jobName() throws Exception {
        // TODO - tidy up constants
        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();

        when(jobsService.getJob("TESTNAME", "TESTID11")).thenReturn(dummyJob);

        mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}", "TESTNAME", "TESTID11")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(dummyJob)));

        verify(jobsService, times(1)).getJob("TESTNAME", "TESTID11");
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    // TODO - refactor with purge with exception?
    public void get_job_with_exception_should_be_converted_to_error_message() throws Exception {
        String errorMessage = "JobId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).getJob(jobName, jobId);

        mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}/", jobName, jobId)).andExpect(status().isIAmATeapot())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_job_files_with_jobId_and_jobName() throws Exception {

        JobFile jesjcl = JobFile.builder().id(3).ddname("JESJCL").recfm("V").lrecl(136).byteCount(182).recordCount(3)
                .build();
        JobFile jesmsglg = JobFile.builder().id(2).ddname("JESMSGLG").recfm("UA").lrecl(133).byteCount(1103)
                .recordCount(20).build();
        List<JobFile> jobFiles = Arrays.asList(jesjcl, jesmsglg);

        String jobName = "TESTNAME";
        String jobId = "TESTID11";
        when(jobsService.getJobFiles(jobName, jobId)).thenReturn(jobFiles);

        mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}/files", jobName, jobId)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(jobFiles)));

        verify(jobsService, times(1)).getJobFiles(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    // TODO - refactor with purge with exception?
    public void get_job_files_with_exception_should_be_converted_to_error_message() throws Exception {
        String errorMessage = "JobId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).getJobFiles(jobName, jobId);

        mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}/files", jobName, jobId)).andExpect(status().isIAmATeapot())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJobFiles(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_job_file_content() throws Exception {

        JobFileContent jobFileContent = new JobFileContent(
                "1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\n          //*        TEST JOB\n        2 //UNIT     EXEC PGM=IEFBR14\n");

        String jobName = "TESTNAME";
        String jobId = "TESTID11";
        String fileId = "3";
        when(jobsService.getJobFileContent(jobName, jobId, fileId)).thenReturn(jobFileContent);

        mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}/files/{fileId}/content", jobName, jobId, fileId))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(jobFileContent)));

        verify(jobsService, times(1)).getJobFileContent(jobName, jobId, fileId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    // TODO - refactor with purge with exception?
    public void get_job_file_content_with_exception_should_be_converted_to_error_message() throws Exception {
        String errorMessage = "FileId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";
        String fileId = "999";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).getJobFileContent(jobName, jobId, fileId);

        mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}/files/{fileId}/content", jobName, jobId, fileId))
                .andExpect(status().isIAmATeapot())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJobFileContent(jobName, jobId, fileId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void purge_job_calls_job_service() throws Exception {
        String jobId = "jobId";
        String jobName = "jobName";

        mockMvc.perform(delete("/api/v1/jobs/{jobName}/{jobId}/", jobName, jobId)).andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(jobsService, times(1)).purgeJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void purge_job_with_exception_should_be_converted_to_error_message() throws Exception {
        String errorMessage = "JobId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).purgeJob(jobName, jobId);

        mockMvc.perform(delete("/api/v1/jobs/{jobName}/{jobId}/", jobName, jobId)).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).purgeJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void submit_jcl_string_works() throws Exception {

        String jobId = "TESTID11";
        String jobName = "TESTNAME";
        Job dummyJob = Job.builder().jobId(jobId).jobName(jobName).status(JobStatus.ACTIVE).build();

        String dummyJcl = "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440\n" + "//*        TEST JOB\n"
                + "//UNIT     EXEC PGM=IEFBR14";
        SubmitJobStringRequest request = new SubmitJobStringRequest(dummyJcl);

        when(jobsService.submitJobString(dummyJcl)).thenReturn(dummyJob);

        URI locationUri = new URI("https://jobURI/jobs/" + jobName + "/" + jobId);
        mockJobUriConstruction(jobName, jobId, locationUri);

        mockMvc.perform(post("/api/v1/jobs/string").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(request))).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(dummyJob)))
                .andExpect(header().string("Location", locationUri.toString()));

        verify(jobsService, times(1)).submitJobString(dummyJcl);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    @Ignore("Re-visit validation later")
    public void submit_jcl_string_with_invalid_jcl_should_be_converted_to_error_message() throws Exception {
        String errorMessage = MessageFormat.format("Invalid field {0} supplied to object {1} - {2}", "jcl",
                "submitJobStringRequest", "JCL string can't be empty");
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.BAD_REQUEST).build();

        mockMvc.perform(post("/api/v1/jobs/string").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(new SubmitJobStringRequest(""))))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void submit_jcl_string_with_exception_should_be_converted_to_error_message() throws Exception {

        String dummyJcl = "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440\n" + "//*        TEST JOB\n"
                + "//UNIT     EXEC PGM=IEFBR14";
        String errorMessage = "Some nonsense about submit failing";
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        when(jobsService.submitJobString(dummyJcl)).thenThrow(new ZoweApiErrorException(expectedError));

        mockMvc.perform(post("/api/v1/jobs/string").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(new SubmitJobStringRequest(dummyJcl))))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).submitJobString(dummyJcl);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void submit_jcl_dataset_works() throws Exception {
        String jobId = "TESTID11";
        String jobName = "TESTNAME";
        Job dummyJob = Job.builder().jobId(jobId).jobName(jobName).status(JobStatus.ACTIVE).build();

        String dummyDataSet = "STEVENH.TEST.JCL(IEFBR14)";
        SubmitJobFileRequest request = new SubmitJobFileRequest(dummyDataSet);

        when(jobsService.submitJobFile(dummyDataSet)).thenReturn(dummyJob);
        URI locationUri = new URI("https://jobURI/jobs/" + jobName + "/" + jobId);
        mockJobUriConstruction(jobName, jobId, locationUri);

        mockMvc.perform(post("/api/v1/jobs/dataset").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(request))).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(JsonUtils.convertToJsonString(dummyJob)))
                .andExpect(header().string("Location", locationUri.toString()));

        verify(jobsService, times(1)).submitJobFile(dummyDataSet);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void submit_jcl_dataset_should_be_converted_to_error_message() throws Exception {

        String dummyDataSet = "INVALID.TEST.JCL(IEFBR14)";
        String errorMessage = "Some nonsense about submit failing";
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        when(jobsService.submitJobFile(dummyDataSet)).thenThrow(new ZoweApiErrorException(expectedError));

        mockMvc.perform(post("/api/v1/jobs/dataset").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(new SubmitJobFileRequest(dummyDataSet))))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).submitJobFile(dummyDataSet);
        verifyNoMoreInteractions(jobsService);
    }

    private void mockJobUriConstruction(String jobName, String jobId, URI uriValue) {
        ServletUriComponentsBuilder servletUriBuilder = mock(ServletUriComponentsBuilder.class);
        PowerMockito.mockStatic(ServletUriComponentsBuilder.class);
        when(ServletUriComponentsBuilder.fromCurrentContextPath()).thenReturn(servletUriBuilder);
        UriComponentsBuilder uriBuilder = mock(UriComponentsBuilder.class);
        when(servletUriBuilder.path("/api/v1/jobs/{jobName}/{jobID}")).thenReturn(uriBuilder);
        UriComponents uriComponents = mock(UriComponents.class);
        when(uriBuilder.buildAndExpand(jobName, jobId)).thenReturn(uriComponents);
        when(uriComponents.toUri()).thenReturn(uriValue);
    }
}
