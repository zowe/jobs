/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2020
 */

package org.zowe.jobs.services.zosmf;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zowe.api.common.connectors.zosmf.ZosmfConnectorJWTAuth;
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.api.common.test.ZoweApiTest;
import org.zowe.jobs.exceptions.InvalidOwnerException;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ZosmfJobsServiceV2.class })
public class ZosmfJobsServiceTest extends ZoweApiTest {

    @Mock
    ZosmfConnectorJWTAuth zosmfConnector;

    ZosmfJobsServiceV2 jobsService;

    @Before
    public void setUp() throws Exception {
        jobsService = new ZosmfJobsServiceV2();
        jobsService.zosmfConnector = zosmfConnector;
    }

    // TODO LATER MAYBE - JUnit 5 parametised the service arguments?
    @Test
    public void testGetJobsRunnerValueCorrectlyReturned() throws Exception {
        String prefix = "prefix";
        String owner = "owner";
        JobStatus status = JobStatus.ACTIVE;

        Job job1 = Job.builder().jobId("1").owner("owner").status(JobStatus.ACTIVE).jobName("prefix1").build();
        Job job2 = Job.builder().jobId("2").owner("owner").status(JobStatus.ACTIVE).jobName("prefix2").build();

        List<Job> jobs = Arrays.asList(job1, job2);
        ItemsWrapper<Job> expected = new ItemsWrapper<Job>(jobs);

        GetJobsZosmfRequestRunner runner = mock(GetJobsZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenReturn(expected);
        PowerMockito.whenNew(GetJobsZosmfRequestRunner.class).withArguments(prefix, owner, status, new ArrayList<>()).thenReturn(runner);
        
        assertEquals(expected, jobsService.getJobs(prefix, owner, status));
    }

    @Test
    public void testGetJobsRunnerExceptionThrown() throws Exception {
        String prefix = "prefix";
        String owner = "owner";
        JobStatus status = JobStatus.ACTIVE;

        ZoweApiRestException expectedException = new InvalidOwnerException(owner);

        GetJobsZosmfRequestRunner runner = mock(GetJobsZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(GetJobsZosmfRequestRunner.class).withArguments(prefix, owner, status, new ArrayList<>()).thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.getJobs(prefix, owner, status));
    }

    @Test
    public void testGetJobRunnerValueCorrectlyReturned() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";

        Job expected = Job.builder().jobId("jobId").jobName("jobName").build();

        GetJobZosmfRequestRunner runner = mock(GetJobZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenReturn(expected);
        PowerMockito.whenNew(GetJobZosmfRequestRunner.class).withArguments(jobName, jobId, new ArrayList<>()).thenReturn(runner);
        assertEquals(expected, jobsService.getJob(jobName, jobId));
    }

    @Test
    public void testGetJobRunnerExceptionThrown() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";

        ZoweApiRestException expectedException = new JobIdNotFoundException("name", "id");

        GetJobZosmfRequestRunner runner = mock(GetJobZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(GetJobZosmfRequestRunner.class).withArguments(jobName, jobId, new ArrayList<>()).thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.getJob(jobName, jobId));
    }

    @Test
    public void testSubmitJobStringRunnerValueCorrectlyReturned() throws Exception {
        String jcl = "jcl";

        Job expected = Job.builder().jobId("jobId").jobName("jobName").build();

        SubmitJobStringZosmfRequestRunner runner = mock(SubmitJobStringZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenReturn(expected);
        PowerMockito.whenNew(SubmitJobStringZosmfRequestRunner.class).withArguments(jcl, new ArrayList<>()).thenReturn(runner);

        assertEquals(expected, jobsService.submitJobString(jcl));
    }

    @Test
    public void testSubmitJobStringRunnerExceptionThrown() throws Exception {
        String jcl = "jcl";

        ZoweApiRestException expectedException = new ZoweApiRestException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Submit input data does not start with a slash");

        SubmitJobStringZosmfRequestRunner runner = mock(SubmitJobStringZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(SubmitJobStringZosmfRequestRunner.class).withArguments(jcl, new ArrayList<>()).thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.submitJobString(jcl));
    }

    @Test
    public void testSubmitJobFileRunnerValueCorrectlyReturned() throws Exception {
        String fileName = "aFile";

        Job expected = Job.builder().jobId("jobId1").jobName("jobName").build();

        SubmitJobFileZosmfRequestRunner runner = mock(SubmitJobFileZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenReturn(expected);
        PowerMockito.whenNew(SubmitJobFileZosmfRequestRunner.class).withArguments(fileName, new ArrayList<>()).thenReturn(runner);

        assertEquals(expected, jobsService.submitJobFile(fileName));
    }

    @Test
    public void testSubmitJobFileRunnerExceptionThrown() throws Exception {
        String fileName = "aFile";

        ZoweApiRestException expectedException = new DataSetNotFoundException(fileName);

        SubmitJobFileZosmfRequestRunner runner = mock(SubmitJobFileZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(SubmitJobFileZosmfRequestRunner.class).withArguments(fileName, new ArrayList<>()).thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.submitJobFile(fileName));
    }

    @Test
    public void testPurgeJobRunnerValueCorrectlyReturned() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";

        PurgeJobZosmfRequestRunner runner = mock(PurgeJobZosmfRequestRunner.class);
        PowerMockito.whenNew(PurgeJobZosmfRequestRunner.class).withArguments(jobName, jobId, new ArrayList<>()).thenReturn(runner);
        jobsService.purgeJob(jobName, jobId);

        verify(runner).run(zosmfConnector);
    }

    @Test
    public void testPurgeJobRunnerExceptionThrown() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";

        ZoweApiRestException expectedException = new JobIdNotFoundException(jobName, jobId);

        PurgeJobZosmfRequestRunner runner = mock(PurgeJobZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(PurgeJobZosmfRequestRunner.class).withArguments(jobName, jobId, new ArrayList<>()).thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.purgeJob(jobName, jobId));
    }

    @Test
    public void testGetJobFilesRunnerValueCorrectlyReturned() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";

        JobFile file1 = JobFile.builder().id(1l).build();
        JobFile file2 = JobFile.builder().id(2l).build();

        List<JobFile> jobsFiles = Arrays.asList(file1, file2);
        ItemsWrapper<JobFile> expected = new ItemsWrapper<JobFile>(jobsFiles);

        GetJobFilesZosmfRequestRunner runner = mock(GetJobFilesZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenReturn(expected);
        PowerMockito.whenNew(GetJobFilesZosmfRequestRunner.class).withArguments(jobName, jobId, new ArrayList<>()).thenReturn(runner);
        assertEquals(expected, jobsService.getJobFiles(jobName, jobId));
    }

    @Test
    public void testGetJobFilesRunnerExceptionThrown() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";

        ZoweApiRestException expectedException = new JobIdNotFoundException("name", "id");

        GetJobFilesZosmfRequestRunner runner = mock(GetJobFilesZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(GetJobFilesZosmfRequestRunner.class).withArguments(jobName, jobId, new ArrayList<>()).thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.getJobFiles(jobName, jobId));
    }

    @Test
    public void testGetJobFileContentRunnerValueCorrectlyReturned() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";
        String fileId = "1";

        JobFileContent expected = new JobFileContent("content");

        GetJobFileContentZosmfRequestRunner runner = mock(GetJobFileContentZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenReturn(expected);
        PowerMockito.whenNew(GetJobFileContentZosmfRequestRunner.class).withArguments(jobName, jobId, fileId, new ArrayList<>())
            .thenReturn(runner);
        assertEquals(expected, jobsService.getJobFileContent(jobName, jobId, fileId));
    }

    @Test
    public void testGetJobFileContentRunnerExceptionThrown() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";
        String fileId = "1";

        ZoweApiRestException expectedException = new JobFileIdNotFoundException(jobName, jobId, fileId);

        GetJobFileContentZosmfRequestRunner runner = mock(GetJobFileContentZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(GetJobFileContentZosmfRequestRunner.class).withArguments(jobName, jobId, fileId, new ArrayList<>())
            .thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.getJobFileContent(jobName, jobId, fileId));
    }

    @Test
    public void get_job_jcl_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21489";
        JobFileContent expected = new JobFileContent(
                "        1 //ATLJ0000 JOB (ADL),'ATLAS',MSGCLASS=X,CLASS=A,TIME=1440               JOB21849\n"
                        + "          //*        TEST JOB\n        2 //UNIT     EXEC PGM=IEFBR14\n" + "");

        GetJobFileContentZosmfRequestRunner runner = mock(GetJobFileContentZosmfRequestRunner.class);
        PowerMockito.whenNew(GetJobFileContentZosmfRequestRunner.class).withArguments(jobName, jobId, "3", new ArrayList<>())
            .thenReturn(runner);

        when(runner.run(zosmfConnector)).thenReturn(expected);

        assertEquals(expected, jobsService.getJobJcl(jobName, jobId));
    }

    @Test
    public void get_job_jcl_for_non_existing_jobname_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        getJobJclCheckException(jobName, jobId, expectedException, expectedException);
    }

    @Test
    public void get_job_jcl_for_non_existing_job_id_should_throw_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "z000000";

        Exception expectedException = new JobIdNotFoundException(jobName, jobId);

        getJobJclCheckException(jobName, jobId, expectedException, expectedException);
    }

    @Test
    public void get_job_jcl_for_non_existing_field_id_should_throw_different_exception() throws Exception {
        String jobName = "ATLJ0000";
        String jobId = "JOB21849";

        Exception expectedException = new JobJesjclNotFoundException(jobName, jobId);
        JobFileIdNotFoundException subException = new JobFileIdNotFoundException(jobName, jobId, "3");

        getJobJclCheckException(jobName, jobId, expectedException, subException);
    }

    private void getJobJclCheckException(String jobName, String jobId, Exception expectedException,
            Exception subException) throws Exception {
        GetJobFileContentZosmfRequestRunner runner = mock(GetJobFileContentZosmfRequestRunner.class);
        PowerMockito.whenNew(GetJobFileContentZosmfRequestRunner.class).withArguments(jobName, jobId, "3", new ArrayList<>())
            .thenReturn(runner);

        when(runner.run(zosmfConnector)).thenThrow(subException);

        shouldThrow(expectedException, () -> jobsService.getJobJcl(jobName, jobId));
    }
    
    @Test
    public void testModifyJobRunnerValueCorrectlyReturned() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";
        String modifyCommand = "cancel";

        ModifyJobZosmfRequestRunner runner = mock(ModifyJobZosmfRequestRunner.class);
        PowerMockito.whenNew(ModifyJobZosmfRequestRunner.class).withArguments(jobName, jobId, modifyCommand, new ArrayList<>())
            .thenReturn(runner);
        jobsService.modifyJob(jobName, jobId, modifyCommand);

        verify(runner).run(zosmfConnector);
    }
    
    @Test
    public void testModifyJobRequestRunnerExceptionThrown() throws Exception {
        String jobName = "jobName";
        String jobId = "jobId";
        String modifyCommand = "cancel";

        ZoweApiRestException expectedException = new JobIdNotFoundException(jobName, jobId);

        ModifyJobZosmfRequestRunner runner = mock(ModifyJobZosmfRequestRunner.class);
        when(runner.run(zosmfConnector)).thenThrow(expectedException);
        PowerMockito.whenNew(ModifyJobZosmfRequestRunner.class).withArguments(jobName, jobId, modifyCommand, new ArrayList<>())
            .thenReturn(runner);

        shouldThrow(expectedException, () -> jobsService.modifyJob(jobName, jobId, modifyCommand));
    }
    
    @Test
    public void testGetIbmHeadersFromRequest() throws Exception {
        List<Header> testHeaders = new ArrayList<Header>();
        testHeaders.add(new BasicHeader("X-IBM-ONE", "test"));
        testHeaders.add(new BasicHeader("X-IBM-TWO", "test2"));
        testHeaders.add(new BasicHeader("X-TEST-TWO", "test3"));
        
        List<String> headerNames = new ArrayList<String>();
        headerNames.add("X-IBM-ONE");
        headerNames.add("X-IBM-TWO");
        Enumeration<String> enumerationHeaderNames = Collections.enumeration(headerNames); 
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        jobsService.setRequest(request);
        
        when(request.getHeaderNames()).thenReturn(enumerationHeaderNames);
        request = mockRequestGetHeaders(testHeaders, request);
        
        List<Header> expectedHeaders = new ArrayList<Header>();
        expectedHeaders.add(new BasicHeader("X-IBM-ONE", "test"));
        expectedHeaders.add(new BasicHeader("X-IBM-TWO", "test2"));
        assertTrue("Actual headers do not match Expected", testHeadersMatch(jobsService.getIbmHeadersFromRequest(), expectedHeaders));
    }
    
    public HttpServletRequest mockRequestGetHeaders(List<Header> headers, HttpServletRequest request) {
        for (Header header : headers) {
            when(request.getHeader(header.getName())).thenReturn(header.getValue());
        }
        return request;
    }
    
    public boolean testHeadersMatch(List<Header> list, List<Header> expectedHeaders) {
        if (list.size() != expectedHeaders.size()) { return false; } 
        for (int i = 0; i < list.size(); i++) {
            BasicHeader header1 = (BasicHeader) list.get(i);
            BasicHeader header2 = (BasicHeader) expectedHeaders.get(i);
            if (header1.getName() != header2.getName()  || header1.getValue() != header2.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    @Test
    public void testGetIbmHeadersFromRequestNullRequest() throws Exception {
        assertEquals(jobsService.getIbmHeadersFromRequest(), new ArrayList<Header>());
    }
}
