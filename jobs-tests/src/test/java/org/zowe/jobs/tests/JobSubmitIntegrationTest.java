/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2019
 */

package org.zowe.jobs.tests;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.zowe.api.common.connectors.zosmf.exceptions.DataSetNotFoundException;
import org.zowe.api.common.errors.ApiError;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.jobs.model.Job;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;

public class JobSubmitIntegrationTest extends AbstractJobsIntegrationTest {

    @Test
    public void testSubmitJobByString() throws Exception {
        submitJclStringAndVerifyJob(JOB_IEFBR14);
    }

    private void submitJclStringAndVerifyJob(String fileString) throws Exception {
        ValidatableResponse response = submitJobJclStringFromFile(fileString).then().statusCode(HttpStatus.SC_CREATED);
        verifyJob(response);
    }

    private void verifyJob(ValidatableResponse response) throws Exception {
        Job actual = response.extract().body().as(Job.class);
        String jobName = actual.getJobName();
        String jobId = actual.getJobId();

        verifyInProgressJobIsAsExpected(actual);
        response.header("Location", endsWith(JOBS_SERVICE_ID + "/" + jobName + "/" + jobId));
    }

    @Test
    public void testSubmitJobByStringWithBadJcl() throws Exception {
        ApiError expectedError = ApiError.builder().status(org.springframework.http.HttpStatus.BAD_REQUEST)
            .message("Job input was not recognized by system as a job").build();

        submitJobJclString("//Some bad jcl").then().statusCode(expectedError.getStatus().value())
            .contentType(ContentType.JSON).body("status", equalTo(expectedError.getStatus().name()))
            .body("message", equalTo(expectedError.getMessage()));
    }

    @Test
    public void testSubmitJobByStringWithEmptyJcl() throws Exception {
        ApiError expectedError = ApiError.builder().status(org.springframework.http.HttpStatus.BAD_REQUEST)
            .message("Invalid field jcl supplied to object submitJobStringRequest - JCL string can't be empty").build();
        submitJobJclString("").then().statusCode(expectedError.getStatus().value()).contentType(ContentType.JSON)
            .body("status", equalTo(expectedError.getStatus().name()))
            .body("message", equalTo(expectedError.getMessage()));
    }

//     TODO LATER - test submitting other invalid JCL (eg line > 72)

    @Test
    @Ignore("see todo") // TODO - need to make build environment set up dataset
    public void testSubmitJobDataSet() throws Exception {
        String dataSetPath = getTestJclMemberPath(JOB_IEFBR14);
        submitAndVerifyJob("'" + dataSetPath + "'");
    }

    @Test
    public void testPostJobInvalidJobDataSet() throws Exception {
        String dataSet = "ATLAS.TEST.JCL(INVALID)";
        ZoweApiRestException expected = new DataSetNotFoundException(dataSet);

        verifyExceptionReturn(expected, submitJobByFile(dataSet));
    }

//    @Test
//    public void testPostJobFromUSS() throws Exception {
//        String submitJobUssPath = USER_DIRECTORY + "/submitJob";
//        createUssFileWithJobIfNecessary(submitJobUssPath);
//        submitAndVerifySuccessfulJob(submitJobUssPath);
//    }

    static String getTestJclMemberPath(String member) {
        return USER.toUpperCase() + ".TEST.JCL(" + member + ")";
    }

//    private void createUssFileWithJobIfNecessary(String submitJobUssPath) throws Exception {
//        if (getAttributes(submitJobUssPath).getStatus() != HttpStatus.SC_OK) {
//            createFile(submitJobUssPath, null);
//            String jobContent = new String(Files.readAllBytes(Paths.get("testFiles/jobIEFBR14")));
//            updateFileContent(submitJobUssPath, jobContent, null);
//        }
//    }
//

    private void submitAndVerifyJob(String fileString) throws Exception {
        verifyInProgressJobIsAsExpected(submitJobByFile(fileString).then().extract().body().as(Job.class));
    }
}
