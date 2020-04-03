/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2020
 */
package org.zowe.jobs.controller;

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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.api.common.errors.ApiError;
import org.zowe.api.common.exceptions.ZoweApiErrorException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.api.common.test.controller.ApiControllerTest;
import org.zowe.api.common.utils.JsonUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobStepsNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.JobStep;
import org.zowe.jobs.model.ModifyJobRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServletUriComponentsBuilder.class })
public class JobsControllerTest extends ApiControllerTest {

    private static final String ENDPOINT_ROOT = "/api/v2/jobs";

    @Mock
    private JobsService jobsService;

    @InjectMocks
    private JobsControllerV2 jobsController;

    @Override
    public Object getController() {
        return jobsController;
    }

    // TODO LATER - job Name and prefix validation - https://github.com/zowe/jobs/issues/10?
    @Test
    public void test_get_jobs_with_owner_and_prefix_works() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);
        ItemsWrapper<Job> items = new ItemsWrapper<Job>(jobs);

        when(jobsService.getJobs("TESTNAME", "*", JobStatus.ALL)).thenReturn(items);

        mockMvc.perform(get(ENDPOINT_ROOT + "?prefix={prefix}&owner={owner}", "TESTNAME", "*"))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string(JsonUtils.convertToJsonString(items)));

        verify(jobsService, times(1)).getJobs("TESTNAME", "*", JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_jobs_with_owner_and_prefix_and_status_works() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);
        ItemsWrapper<Job> items = new ItemsWrapper<Job>(jobs);

        when(jobsService.getJobs("TESTNAME", "*", JobStatus.ACTIVE)).thenReturn(items);

        mockMvc
            .perform(get(ENDPOINT_ROOT + "?prefix={prefix}&owner={owner}&status={status}", "TESTNAME", "*",
                    JobStatus.ACTIVE))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string(JsonUtils.convertToJsonString(items)));

        verify(jobsService, times(1)).getJobs("TESTNAME", "*", JobStatus.ACTIVE);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_jobs_with_no_owner_defaults() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);
        ItemsWrapper<Job> items = new ItemsWrapper<Job>(jobs);

        when(jobsService.getJobs("TESTNAME", null, JobStatus.ALL)).thenReturn(items);

        mockMvc.perform(get(ENDPOINT_ROOT + "?prefix={prefix}", "TESTNAME")).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string(JsonUtils.convertToJsonString(items)));

        verify(jobsService, times(1)).getJobs("TESTNAME", null, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_jobs_with_no_results_returns_empty_body() throws Exception {

        ItemsWrapper<Job> items = new ItemsWrapper<Job>(Collections.emptyList());

        when(jobsService.getJobs("TESTNAME", null, JobStatus.ALL)).thenReturn(items);

        mockMvc.perform(get(ENDPOINT_ROOT + "?prefix={prefix}", "TESTNAME")).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string(EMPTY_ITEMS));

        verify(jobsService, times(1)).getJobs("TESTNAME", null, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void get_jobs_with_exception_should_be_converted_to_error_message() throws Exception {

        String invalidOwner = "DUMMY_OWNER";

        String errorMessage = MessageFormat.format("An invalid job owner of ''{0}'' was supplied", invalidOwner);
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.BAD_REQUEST).build();

        InvalidOwnerException zoweException = new InvalidOwnerException(invalidOwner);
        when(jobsService.getJobs("TESTNAME", invalidOwner, JobStatus.ALL)).thenThrow(zoweException);

        mockMvc.perform(get(ENDPOINT_ROOT + "?prefix={prefix}&owner={owner}", "TESTNAME", invalidOwner))
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

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}", "TESTNAME", "TESTID11")).andExpect(status().isOk())
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

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/", jobName, jobId)).andExpect(status().isIAmATeapot())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    public void checkExceptionConvertedToError() throws Exception {
        String errorMessage = "JobId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).getJob(jobName, jobId);

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/", jobName, jobId)).andExpect(status().isIAmATeapot())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void test_get_job_files_with_jobId_and_jobName() throws Exception {

        JobFile jesjcl = JobFile.builder().id(3l).ddName("JESJCL").recordFormat("V").recordLength(136l).byteCount(182l)
            .recordCount(3l).build();
        JobFile jesmsglg = JobFile.builder().id(2l).ddName("JESMSGLG").recordFormat("UA").recordLength(133l)
            .byteCount(1103l).recordCount(20l).build();
        List<JobFile> jobFiles = Arrays.asList(jesjcl, jesmsglg);
        ItemsWrapper<JobFile> items = new ItemsWrapper<JobFile>(jobFiles);

        String jobName = "TESTNAME";
        String jobId = "TESTID11";
        when(jobsService.getJobFiles(jobName, jobId)).thenReturn(items);

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files", jobName, jobId)).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string(JsonUtils.convertToJsonString(items)));

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

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files", jobName, jobId))
            .andExpect(status().isIAmATeapot()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files/{fileId}/content", jobName, jobId, fileId))
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

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files/{fileId}/content", jobName, jobId, fileId))
            .andExpect(status().isIAmATeapot()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJobFileContent(jobName, jobId, fileId);
        verifyNoMoreInteractions(jobsService);
    }
    
    @Test
    public void get_concatenated_job_files_content() throws Exception {
        JobFile jesjcl = JobFile.builder().id(3l).ddName("JESJCL").recordFormat("V").recordLength(136l).byteCount(182l)
                .recordCount(3l).build();
        JobFile jesmsglg = JobFile.builder().id(2l).ddName("JESMSGLG").recordFormat("UA").recordLength(133l)
            .byteCount(1103l).recordCount(20l).build();
        List<JobFile> jobFiles = Arrays.asList(jesjcl, jesmsglg);
        ItemsWrapper<JobFile> items = new ItemsWrapper<JobFile>(jobFiles);
        
        String jobId = "jobId";
        String jobName = "jobName";
        when(jobsService.getJobFiles(jobName, jobId)).thenReturn(items);
        
        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files", jobName, jobId)).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(content().string(JsonUtils.convertToJsonString(items)));
        
        JobFileContent jobFileContent1 = new JobFileContent("1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\\n");
        JobFileContent jobFileContent2 = new JobFileContent("Job completed");
        
        String fileId1 = "3";
        String fileId2 = "2";
        when(jobsService.getJobFileContent(jobName, jobId, fileId1)).thenReturn(jobFileContent1);
        when(jobsService.getJobFileContent(jobName, jobId, fileId2)).thenReturn(jobFileContent2);
        
        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files/{fileId}/content", jobName, jobId, fileId1))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(content().string(JsonUtils.convertToJsonString(jobFileContent1)));
        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files/{fileId}/content", jobName, jobId, fileId2))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(content().string(JsonUtils.convertToJsonString(jobFileContent2)));
        
        JobFileContent concatenatedContent = new JobFileContent(jobFileContent1.getContent() + jobFileContent2.getContent());
        
        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/files/content", jobName, jobId))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(content().string(JsonUtils.convertToJsonString(concatenatedContent)));
    }

    @Test
    public void test_get_job_steps_with_jobId_and_jobName() throws Exception {

        JobStep step1 = JobStep.builder().name("STEP1").program("IEBGENER").step(1).build();
        JobStep step2 = JobStep.builder().name("STEP2").program("AOPBATCH").step(2).build();

        List<JobStep> expected = Arrays.asList(step1, step2);

        String jobName = "TESTNAME";
        String jobId = "TESTID11";

        when(jobsService.getJobJcl(jobName, jobId))
            .thenReturn(new JobFileContent(loadFile("src/test/resources/testData/JESJCL")));

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/steps", jobName, jobId)).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string(JsonUtils.convertToJsonString(expected)));

        verify(jobsService, times(1)).getJobJcl(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    // TODO - refactor with purge with exception?
    public void get_job_steps_with_exception_should_be_converted_to_error_message() throws Exception {
        String errorMessage = "JobId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).getJobJcl(jobName, jobId);

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/steps", jobName, jobId))
            .andExpect(status().isIAmATeapot()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).getJobJcl(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    // TODO - refactor with purge with exception?
    public void get_job_steps_with_jesjcl_exception_should_be_converted_to_error_message() throws Exception {

        String jobId = "jobId";
        String jobName = "jobName";

        JobStepsNotFoundException expectedException = new JobStepsNotFoundException(jobName, jobId);

        ApiError expectedError = expectedException.getApiError();

        doThrow(new JobJesjclNotFoundException(jobName, jobId)).when(jobsService).getJobJcl(jobName, jobId);

        mockMvc.perform(get(ENDPOINT_ROOT + "/{jobName}/{jobId}/steps", jobName, jobId))
            .andExpect(status().isIAmATeapot()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(expectedError.getMessage()));

        verify(jobsService, times(1)).getJobJcl(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }

    @Test
    public void purge_job_calls_job_service() throws Exception {
        String jobId = "jobId";
        String jobName = "jobName";

        mockMvc.perform(delete(ENDPOINT_ROOT + "/{jobName}/{jobId}/", jobName, jobId)).andExpect(status().isNoContent())
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

        mockMvc.perform(delete(ENDPOINT_ROOT + "/{jobName}/{jobId}/", jobName, jobId))
            .andExpect(status().isIAmATeapot()).andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(errorMessage));

        verify(jobsService, times(1)).purgeJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
    }
    
    @Test
    public void modify_job_calls_job_service() throws Exception {
        String jobName = "TESTJOB";
        String jobId = "JOB12345";
        
        ModifyJobRequest request = new ModifyJobRequest("cancel");
        
        mockMvc.perform(put(ENDPOINT_ROOT + "/{jobName}/{jobId}", jobName, jobId)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(JsonUtils.convertToJsonString(request)))
                .andExpect(status().isAccepted());
        
        verify(jobsService, times(1)).modifyJob(jobName, jobId, request.getCommand());
        verifyNoMoreInteractions(jobsService);
    }
    
    @Test
    public void modify_job_with_exceptions_sould_be_converted_to_error_message() throws Exception {
        String errorMessage = "JobId could not be found";
        
        String jobId = "job1234";
        String jobName = "TESTJOB";
        
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.NOT_FOUND).build();

        ModifyJobRequest request = new ModifyJobRequest("cancel");
        
        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).modifyJob(jobName, jobId, request.getCommand());
        
        mockMvc.perform(put(ENDPOINT_ROOT + "/{jobName}/{jobId}", jobName, jobId)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(JsonUtils.convertToJsonString(request)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
            .andExpect(jsonPath("$.message").value(errorMessage));
        
        verify(jobsService, times(1)).modifyJob(jobName, jobId, request.getCommand());
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

        mockMvc
            .perform(post(ENDPOINT_ROOT + "/string").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(request)))
            .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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

        mockMvc
            .perform(post(ENDPOINT_ROOT + "/string").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
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

        mockMvc
            .perform(post(ENDPOINT_ROOT + "/string").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
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

        mockMvc
            .perform(post(ENDPOINT_ROOT + "/dataset").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(JsonUtils.convertToJsonString(request)))
            .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
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

        mockMvc
            .perform(post(ENDPOINT_ROOT + "/dataset").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
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
        when(servletUriBuilder.path(ENDPOINT_ROOT + "/{jobName}/{jobID}")).thenReturn(uriBuilder);
        UriComponents uriComponents = mock(UriComponents.class);
        when(uriBuilder.buildAndExpand(jobName, jobId)).thenReturn(uriComponents);
        when(uriComponents.toUri()).thenReturn(uriValue);
    }
}
