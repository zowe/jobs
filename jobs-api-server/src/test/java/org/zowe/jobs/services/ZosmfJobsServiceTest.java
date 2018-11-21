/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.jobs.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
import org.zowe.api.common.test.ZoweApiTest;
import org.zowe.api.common.utils.JsonUtils;
import org.zowe.api.common.utils.ResponseUtils;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.InvalidPrefixException;
import org.zowe.jobs.exceptions.NoZosmfResponseEntityException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ResponseUtils.class, ZosmfJobsService.class, RequestBuilder.class, JsonUtils.class,
        ContentType.class})
public class ZosmfJobsServiceTest extends ZoweApiTest {

    private static final String BASE_URL = "https://dummy.com/zosmf/";

    @Mock
    ZosmfConnector zosmfConnector;

    ZosmfJobsService jobsService;

    @Before
    public void setUp() throws Exception {
        jobsService = new ZosmfJobsService();
        jobsService.zosmfconnector = zosmfConnector;
        when(zosmfConnector.getFullUrl(anyString())).thenAnswer(new org.mockito.stubbing.Answer<String>() {
            @Override
            public String answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return BASE_URL + (String) args[0];
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

        verifyInteractions(requestBuilder);
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
        checkExceptionThrownAndVerifyCalls(prefix, owner, expectedException, response);
    }

    @Test
    public void get_jobs_with_no_response_entity_from_zosmf_should_throw_exception() throws Exception {

        String prefix = "PREFIX";
        String owner = "OWNER";
        String path = String.format("restjobs/jobs?owner=%s&prefix=%s", owner, prefix);
        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.I_AM_A_TEAPOT;

        Exception expectedException = new NoZosmfResponseEntityException(status, path);

        HttpResponse response = mockResponse(status.value());
        checkExceptionThrownAndVerifyCalls(prefix, owner, expectedException, response);
    }

    private void checkExceptionThrownAndVerifyCalls(String prefix, String owner, Exception expectedException,
                                                    HttpResponse response) throws IOException, Exception {

        String path = String.format("restjobs/jobs?owner=%s&prefix=%s", owner, prefix);
        RequestBuilder requestBuilder = mockGetBuilder(path);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> jobsService.getJobs(prefix, owner, JobStatus.ALL));

        verifyInteractions(requestBuilder);
    }

    private void verifyInteractions(RequestBuilder requestBuilder) throws IOException {
        verify(zosmfConnector, times(1)).request(requestBuilder);
        verify(zosmfConnector, times(1)).getFullUrl(anyString());
        verifyNoMoreInteractions(zosmfConnector);
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

    // TODO LATER - refactor out into common?
    private RequestBuilder mockGetBuilder(String relativeUri) {
        RequestBuilder builder = mock(RequestBuilder.class);
        mockStatic(RequestBuilder.class);
        when(RequestBuilder.get(BASE_URL + relativeUri)).thenReturn(builder);
        return builder;
    }

    private RequestBuilder mockDeleteBuilder(String relativeUri) {
        RequestBuilder builder = mock(RequestBuilder.class);
        mockStatic(RequestBuilder.class);
        when(RequestBuilder.delete(BASE_URL + relativeUri)).thenReturn(builder);
        return builder;
    }

    private RequestBuilder mockPostBuilder(String relativeUri, Object body) throws Exception {
        RequestBuilder builder = mock(RequestBuilder.class);

        String jsonString = "dummyJson";
        mockStatic(JsonUtils.class);
        when(JsonUtils.convertToJsonString(body)).thenReturn(jsonString);
        return mockPostBuilder(relativeUri, jsonString);
    }

    private RequestBuilder mockPostBuilder(String relativeUri, String jsonString) throws Exception {
        RequestBuilder builder = mock(RequestBuilder.class);

        StringEntity stringEntity = mock(StringEntity.class);
        PowerMockito.whenNew(StringEntity.class).withArguments(jsonString, Charset.forName("UTF-8"))
                .thenReturn(stringEntity);

        mockStatic(RequestBuilder.class);
        when(RequestBuilder.post(BASE_URL + relativeUri)).thenReturn(builder);
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

    private HttpResponse mockResponse(int statusCode) throws IOException {
        HttpResponse response = mock(HttpResponse.class);
        mockStatic(ResponseUtils.class);
        when(ResponseUtils.getStatus(response)).thenReturn(statusCode);
        return response;
    }

    public String loadTestFile(String relativePath) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get("src/test/resources/zosmfResponses/" + relativePath));
        return new String(encoded, Charset.forName("UTF8"));
    }
}
