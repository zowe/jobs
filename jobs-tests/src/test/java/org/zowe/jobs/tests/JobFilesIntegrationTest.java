/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package org.zowe.jobs.tests;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;
import org.zowe.tests.IntegrationTestResponse;

//TODO - rewrite using rest assured
public class JobFilesIntegrationTest extends AbstractJobsIntegrationTest {

    private static Job job;

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
    }

    @AfterClass
    public static void purgeJob() throws Exception {
        purgeJob(job);
    }

    /**
     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files
     */
    @Test
    public void testGetJobOutputFiles() throws Exception {
        System.out.println("> testGetJobOutputFiles()");

        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/ids/files/files_regex.txt";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
    }

    @Test
    public void testGetJobOutputFilesInvalidJobId() throws Exception {
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), "z000000"))
                .build();

        getJobFiles(job.getJobName(), "z000000").shouldReturnError(expected);
    }

    @Test
    public void testGetJobOutputFilesInvalidJobNameAndId() throws Exception {
        // TODO MAYBE - use exception?
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), "z000000"))
                .build();

        getJobFiles("z", "z000000").shouldReturnError(expected);
    }

    public static IntegrationTestResponse getJobFiles(String jobName, String jobId) throws Exception {
        return sendGetRequest2(getJobUri(jobName, jobId) + "/files");
    }

    /**
     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files{fieldId}
     */
//    @Test
//    public void testGetJobOutputFileFieldId() throws Exception {
//        System.out.println("> testGetJobOutputFileFieldId()");
//
//        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files/2";
//        String httpMethodType = HttpGet.METHOD_NAME;
//        String expectedResultFilePath = "expectedResults/Jobs/ids/files/JESMSGLG_regex.txt";
//        int expectedReturnCode = HttpStatus.SC_OK;
//
//        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
//    }
//
//    /**
//     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files{fieldId}
//     */
//    @Test
//    public void testGetJobOutputFileFieldIdStartParam() throws Exception {
//        System.out.println("> testGetJobOutputFileFieldIdStartParam()");
//
//        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files/2?start=2";
//        String httpMethodType = HttpGet.METHOD_NAME;
//        String expectedResultFilePath = "expectedResults/Jobs/ids/files/JESMSGLG_regex.txt";
//        int expectedReturnCode = HttpStatus.SC_OK;
//
//        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
//    }
//
//    /**
//     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files{fieldId}
//     */
//    @Test
//    public void testGetJobOutputFileFieldIdEndParam() throws Exception {
//        System.out.println("> testGetJobOutputFileFieldIdEndParam()");
//
//        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files/2?end=0";
//        String httpMethodType = HttpGet.METHOD_NAME;
//        String expectedResultFilePath = "expectedResults/Jobs/ids/files/JESMSGLG_regex.txt";
//        int expectedReturnCode = HttpStatus.SC_OK;
//
//        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
//    }
//
//    /**
//     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files{fieldId}
//     */
//    @Test
//    public void testGetJobOutputFileFieldIdStartEndParam() throws Exception {
//        System.out.println("> testGetJobOutputFileFieldIdStartEndParam()");
//
//        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files/2?start=0&end=1";
//        String httpMethodType = HttpGet.METHOD_NAME;
//        String expectedResultFilePath = "expectedResults/Jobs/ids/files/JESMSGLG_line1.json";
//        int expectedReturnCode = HttpStatus.SC_OK;
//
//        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode);
//    }
//
//    /**
//     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files/{fileId}/tail
//     */
//    @Test
//    public void testGetJobOutputFileFieldIdTail() throws Exception {
//        System.out.println("> testGetJobOutputFileFieldIdTail()");
//
//        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files/2/tail";
//        String httpMethodType = HttpGet.METHOD_NAME;
//        String expectedResultFilePath = "expectedResults/Jobs/ids/files/JESMSGLG_regex.txt";
//        int expectedReturnCode = HttpStatus.SC_OK;
//
//        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
//    }
//
//    /**
//     * GET /Atlas/jobs/{jobName}/ids/{jobId}/files/{fileId}/tail
//     */
//    @Test
//    public void testGetJobOutputFileFieldIdTailRecords() throws Exception {
//        System.out.println("> testGetJobOutputFileFieldIdTailRecords()");
//
//        String relativeURI = "jobs/" + job.getJobName() + "/ids/" + job.getJobId() + "/files/2/tail?records=1";
//        String httpMethodType = HttpGet.METHOD_NAME;
//        String expectedResultFilePath = "expectedResults/Jobs/ids/files/JESMSGLG_tail1.json";
//        int expectedReturnCode = HttpStatus.SC_OK;
//
//        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
//    }

}