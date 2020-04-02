/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019, 2020
 */
package org.zowe.jobs.services.zosmf;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.zowe.jobs.exceptions.JobNameNotFoundException;

import static org.mockito.Mockito.when;


@PrepareForTest({ ModifyJobZosmfRequestRunner.class })
public class ModifyJobZosmfRequestRunnerTest extends AbstractZosmfJobsRequestRunnerTest {
    
    @Test
    public void modify_job_should_call_zozmf_correctly() throws Exception {
        String jobName = "TESTJOB";
        String jobId = "JOB12345";
        String command = "cancel";
        
        mockResponseCache(HttpStatus.SC_ACCEPTED);
        
        JsonObject body = new JsonObject();
        body.addProperty("request", command);
        RequestBuilder requestBuilder = mockPutBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId), body);
                
        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);
        
        new ModifyJobZosmfRequestRunner(jobName, jobId, command).run(zosmfConnector);
        
        verifyInteractions(requestBuilder);
    }
    
    @Test
    public void modify_job_for_non_existing_job_should_parse_and_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";
        String command = "cancel";
        
        Exception expectedException = new JobNameNotFoundException(jobName, jobId);
        
        mockJsonResponse(HttpStatus.SC_BAD_REQUEST, loadTestFile("zosmf_getJob_noJobNameResponse.json"));
        
        JsonObject body = new JsonObject();
        body.addProperty("request", command);
        RequestBuilder requestBuilder = mockPutBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId), body);
        
        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);
        
        shouldThrow(expectedException, () -> new ModifyJobZosmfRequestRunner(jobName, jobId, command).run(zosmfConnector));
        
        verifyInteractions(requestBuilder);
    }
}
