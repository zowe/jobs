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

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.v2.services.zosmf.SubmitJobFileZosmfRequestRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@PrepareForTest({ SubmitJobFileZosmfRequestRunner.class })
public class SubmitJobFileZosmfRequestRunnerTest extends AbstractZosmfJobsRequestRunnerTest {

    @Test
    public void submit_job_file_should_call_zosmf_and_parse_response_correctly() throws Exception {
        String dataSet = "STEVENH.TEST.JCL(IEFBR14)";

        Job expected = createJob("STC16867", "ZOEJC", "IZUSVR", "STC", JobStatus.OUTPUT,
                "Job is on the hard copy queue", "CANCELED");

        mockJsonResponse(HttpStatus.SC_CREATED, loadTestFile("zosmf_getJobResponse.json"));

        // TODO MAYBE map zosmf model object?
        JsonObject body = new JsonObject();
        body.addProperty("file", "//'" + dataSet + "'");
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", body);

        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        assertEquals(expected, new SubmitJobFileZosmfRequestRunner(dataSet).run(zosmfConnector));

        verifyInteractions(requestBuilder);
    }

    @Test
    public void submit_job_file_with_no_member_call_zosmf_parse_and_throw_exception() throws Exception {
        String dataSet = "STEVENH.TEST.JCL(INVALID)";
        Exception expectedException = new DataSetNotFoundException(dataSet);

        checkSubmitJobFileExceptionAndVerify(dataSet, expectedException, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "zosmf_submitJobByFile_noDatasetMember.json");
    }

    @Test
    public void submit_job_file_with_no_data_set_call_zosmf_parse_and_throw_exception() throws Exception {
        String dataSet = "INVALID.TEST.JCL(IEFBR14)";
        Exception expectedException = new DataSetNotFoundException(dataSet);

        checkSubmitJobFileExceptionAndVerify(dataSet, expectedException, HttpStatus.SC_BAD_REQUEST,
                "zosmf_submitJobByFile_noDataset.json");
    }

    private void checkSubmitJobFileExceptionAndVerify(String fileName, Exception expectedException, int statusCode,
            String file) throws IOException, Exception {

        mockJsonResponse(statusCode, loadTestFile(file));

        JsonObject body = new JsonObject();
        body.addProperty("file", "//'" + fileName + "'");
        RequestBuilder requestBuilder = mockPutBuilder("restjobs/jobs", body);
        when(zosmfConnector.request(requestBuilder)).thenReturn(response);

        shouldThrow(expectedException, () -> new SubmitJobFileZosmfRequestRunner(fileName).run(zosmfConnector));
        verifyInteractions(requestBuilder);
    }

}
