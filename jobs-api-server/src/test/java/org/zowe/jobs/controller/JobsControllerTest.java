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
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.api.common.errors.ApiError;
import org.zowe.api.common.exceptions.ZoweApiErrorException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.exceptions.ZoweRestExceptionHandler;
import org.zowe.api.common.test.ZoweApiTest;
import org.zowe.api.common.utils.JsonUtils;
import org.zowe.api.common.utils.ZosUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.model.SubmitJobStringRequest;
import org.zowe.jobs.services.JobsService;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ZosUtils.class })
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

        String expectedJsonString = JsonUtils.convertToJsonString(jobs);

        when(jobsService.getJobs("TESTNAME", "*", JobStatus.ALL)).thenReturn(jobs);

        MvcResult result = mockMvc.perform(get("/api/v1/jobs?prefix={prefix}&owner={owner}", "TESTNAME", "*"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();

        verify(jobsService, times(1)).getJobs("TESTNAME", "*", JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void test_get_jobs_with_owner_and_prefix_and_status_works() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);

        String expectedJsonString = JsonUtils.convertToJsonString(jobs);

        when(jobsService.getJobs("TESTNAME", "*", JobStatus.ACTIVE)).thenReturn(jobs);

        MvcResult result = mockMvc
                .perform(get("/api/v1/jobs?prefix={prefix}&owner={owner}&status={status}", "TESTNAME", "*",
                        JobStatus.ACTIVE))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();

        verify(jobsService, times(1)).getJobs("TESTNAME", "*", JobStatus.ACTIVE);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void test_get_jobs_with_no_owner_defaults() throws Exception {

        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        Job dummyJob2 = Job.builder().jobId("TESTID12").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        List<Job> jobs = Arrays.asList(dummyJob, dummyJob2);

        String expectedJsonString = JsonUtils.convertToJsonString(jobs);

        when(jobsService.getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL)).thenReturn(jobs);

        MvcResult result = mockMvc.perform(get("/api/v1/jobs?prefix={prefix}", "TESTNAME")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn();

        verify(jobsService, times(1)).getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void test_get_jobs_with_no_results_returns_empty_body() throws Exception {

        List<Job> jobs = Collections.emptyList();

        when(jobsService.getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL)).thenReturn(jobs);

        MvcResult result = mockMvc.perform(get("/api/v1/jobs?prefix={prefix}", "TESTNAME")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn();

        verify(jobsService, times(1)).getJobs("TESTNAME", DUMMY_USER, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals("[]", result.getResponse().getContentAsString(), false);
    }

    @Test
    public void get_jobs_with_exception_should_be_converted_to_error_message() throws Exception {

        String invalidOwner = "DUMMY_OWNER";

        ApiError expectedError = ApiError.builder()
                .message(MessageFormat.format("An invalid job owner of ''{0}'' was supplied", invalidOwner))
                .status(HttpStatus.BAD_REQUEST).build();
        String expectedJsonString = JsonUtils.convertToJsonString(expectedError);

        InvalidOwnerException zoweException = new InvalidOwnerException(invalidOwner);
        when(jobsService.getJobs("TESTNAME", invalidOwner, JobStatus.ALL)).thenThrow(zoweException);

        MvcResult result = mockMvc.perform(get("/api/v1/jobs?prefix={prefix}&owner={owner}", "TESTNAME", invalidOwner))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn();

        verify(jobsService, times(1)).getJobs("TESTNAME", invalidOwner, JobStatus.ALL);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);

    }

    @Test
    public void test_get_job_with_jobId_and_jobName() throws Exception {
        // TODO - tidy up constants
        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        String expectedJsonString = JsonUtils.convertToJsonString(dummyJob);

        when(jobsService.getJob("TESTNAME", "TESTID11")).thenReturn(dummyJob);

        MvcResult result = mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}", "TESTNAME", "TESTID11"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();

        verify(jobsService, times(1)).getJob("TESTNAME", "TESTID11");
        verifyNoMoreInteractions(jobsService);
        // TODO NOW - add assert into expect?
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    // TODO - refactor with purge with exception?
    public void get_job_with_exception_should_be_converted_to_error_message() throws Exception {
        String errorMessage = "JobId could not be found";

        String jobId = "jobId";
        String jobName = "jobName";

        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();
        String expectedJsonString = JsonUtils.convertToJsonString(expectedError);

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).getJob(jobName, jobId);

        // TODO NOW - add assert into expect?
        MvcResult result = mockMvc.perform(get("/api/v1/jobs/{jobName}/{jobId}/", jobName, jobId))
                .andExpect(status().isIAmATeapot())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(expectedError.getStatus().name()))
                .andExpect(jsonPath("$.message").value(errorMessage)).andReturn();

        verify(jobsService, times(1)).getJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);

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
        String expectedJsonString = JsonUtils.convertToJsonString(expectedError);

        doThrow(new ZoweApiErrorException(expectedError)).when(jobsService).purgeJob(jobName, jobId);

        // TODO NOW - add assert into expect?
        MvcResult result = mockMvc.perform(delete("/api/v1/jobs/{jobName}/{jobId}/", jobName, jobId))
                .andExpect(status().isIAmATeapot())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn();

        verify(jobsService, times(1)).purgeJob(jobName, jobId);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void submit_jcl_string_works() throws Exception {
        Job dummyJob = Job.builder().jobId("TESTID11").jobName("TESTNAME").status(JobStatus.ACTIVE).build();
        String expectedJsonString = JsonUtils.convertToJsonString(dummyJob);

        String dummyJcl = "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440\n" + "//*        TEST JOB\n"
                + "//UNIT     EXEC PGM=IEFBR14";
        SubmitJobStringRequest request = new SubmitJobStringRequest(dummyJcl);

        when(jobsService.submitJobString(dummyJcl)).thenReturn(dummyJob);

        MvcResult result = mockMvc
                .perform(post("/api/v1/jobs").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(JsonUtils.convertToJsonString(request)))
                .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();

        verify(jobsService, times(1)).submitJobString(dummyJcl);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    @Ignore("Re-visit validation later")
    public void submit_jcl_string_with_invalid_jcl_should_be_converted_to_error_message() throws Exception {
        ApiError expectedError = ApiError.builder()
                .message(MessageFormat.format("Invalid field {0} supplied to object {1} - {2}", "jcl",
                        "submitJobStringRequest", "JCL string can't be empty"))
                .status(HttpStatus.BAD_REQUEST).build();
        String expectedJsonString = JsonUtils.convertToJsonString(expectedError);

        MvcResult result = mockMvc
                .perform(post("/api/v1/jobs").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(JsonUtils.convertToJsonString(new SubmitJobStringRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn();
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);
    }

    @Test
    public void submit_jcl_string_with_exception_should_be_converted_to_error_message() throws Exception {

        String dummyJcl = "//ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440\n" + "//*        TEST JOB\n"
                + "//UNIT     EXEC PGM=IEFBR14";
        String errorMessage = "Some nonsense about submit failing";
        ApiError expectedError = ApiError.builder().message(errorMessage).status(HttpStatus.I_AM_A_TEAPOT).build();
        String expectedJsonString = JsonUtils.convertToJsonString(expectedError);

        // TODO SOON - make ZoweException with ApiError as constructor
        when(jobsService.submitJobString(dummyJcl))
                .thenThrow(new ZoweApiRestException(HttpStatus.I_AM_A_TEAPOT, errorMessage));

        MvcResult result = mockMvc
                .perform(post("/api/v1/jobs").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(JsonUtils.convertToJsonString(new SubmitJobStringRequest(dummyJcl))))
                .andExpect(status().isIAmATeapot())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn();

        verify(jobsService, times(1)).submitJobString(dummyJcl);
        verifyNoMoreInteractions(jobsService);
        JSONAssert.assertEquals(expectedJsonString, result.getResponse().getContentAsString(), false);

    }
}
