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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.zowe.jobs.exceptions.JobNameNotFoundException;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

public class PurgeJobZosmfRequestRunnerTest extends AbstractZosmfJobsRequestRunnerTest {

    @Test
    public void purge_job_string_should_call_zosmf_correctly() throws Exception {
        String jobName = "AJOB";
        String jobId = "Job12345";

        mockResponseCache(HttpStatus.SC_ACCEPTED);

        RequestBuilder requestBuilder = mockDeleteBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);

        new PurgeJobZosmfRequestRunner(jobName, jobId, new ArrayList<>()).run(zosmfConnector);

        verifyInteractions(requestBuilder);
    }

    @Test
    public void purge_job_for_non_existing_job_should_throw_exception() throws Exception {
        String jobName = "ATLJ5000";
        String jobId = "JOB21489";

        Exception expectedException = new JobNameNotFoundException(jobName, jobId);

        mockJsonResponse(HttpStatus.SC_BAD_REQUEST, loadTestFile("zosmf_getJob_noJobNameResponse.json"));

        RequestBuilder requestBuilder = mockDeleteBuilder(String.format("restjobs/jobs/%s/%s", jobName, jobId));

        when(zosmfConnector.executeRequest(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> new PurgeJobZosmfRequestRunner(jobName, jobId, new ArrayList<>()).run(zosmfConnector));
        verifyInteractions(requestBuilder);
    }

}
