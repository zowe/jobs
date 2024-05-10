/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2020
 */

package org.zowe.jobs.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.apache.http.HttpStatus;
import org.hamcrest.text.MatchesPattern;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobStatus;

import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class JobFilesIntegrationTest extends AbstractJobsIntegrationTest {

    private static Job job;
    private static String expectedContentRegexJESMSGLG = ".*J E S 2  J O B  L O G.*------ JES2 JOB STATISTICS ------.*3 CARDS READ.*"
            + "-           .* SYSOUT PRINT RECORDS.*-            0 SYSOUT PUNCH RECORDS.*"
            + "-            5 SYSOUT SPOOL KBYTES.*-         0.00 MINUTES EXECUTION TIME.*";

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
    }

    @AfterClass
    public static void purgeJob() throws Exception {
        deleteJob(job);
    }

    @Test
    public void testGetJobOutputFiles() throws Exception {
        String jobName = job.getJobName();
        String jobId = job.getJobId();

        JobFile jesmsglg = JobFile.builder().ddName("JESMSGLG").recordFormat("UA").recordLength(133l).id(2l).build();
        JobFile jesjcl = JobFile.builder().ddName("JESJCL").recordFormat("V").recordLength(136l).id(3l).build();
        JobFile jessysmsg = JobFile.builder().ddName("JESYSMSG").recordFormat("VA").recordLength(137l).id(4l).build();

        List<JobFile> actual = getJobFiles(jobName, jobId).then().statusCode(HttpStatus.SC_OK).extract().body()
            .jsonPath().getList("items", JobFile.class);

        // Different systems have different byte and record counts
        for (JobFile jobFile : actual) {
            jobFile.setByteCount(null);
            jobFile.setRecordCount(null);
        }

        assertThat(actual, hasItems(jesmsglg, jesjcl, jessysmsg));
    }

    @Test
    public void testGetJobOutputFilesInvalidJobId() throws Exception {
        String jobName = job.getJobName();
        String jobId = "z0000000";
        verifyExceptionReturn(new JobIdNotFoundException(jobName, jobId), getJobFiles(jobName, jobId));
    }

    @Test
    public void testGetJobOutputFilesInvalidJobNameAndId() throws Exception {
        String jobName = "z";
        String jobId = "z0000000";
        verifyExceptionReturn(new JobIdNotFoundException(jobName, jobId), getJobFiles(jobName, jobId));
    }

    public static Response getJobFiles(String jobName, String jobId) throws Exception {
        return RestAssured.given().header(AUTH_HEADER).when().get(getJobPath(jobName, jobId) + "/files");
    }

    @Test
    public void testGetJobOutputFileContents() throws Exception {
        String jobName = job.getJobName();
        String jobId = job.getJobId();
        
        Pattern regex = Pattern.compile(expectedContentRegexJESMSGLG, Pattern.DOTALL);
        getJobFileContent(jobName, jobId, "2").then().statusCode(HttpStatus.SC_OK).body("content",
                MatchesPattern.matchesPattern(regex));
    }

    @Test
    public void testGetJobOutputFileContentsInvalidJobId() throws Exception {
        String jobName = job.getJobName();
        String jobId = "z0000000";
        verifyExceptionReturn(new JobNameNotFoundException(jobName, jobId), getJobFileContent(jobName, jobId, "2"));
    }

    @Test
    public void testGetJobOutputFileContentsInvalidJobName() throws Exception {
        String jobName = "z";
        String jobId = "z0000000";
        verifyExceptionReturn(new JobIdNotFoundException(jobName, jobId), getJobFileContent(jobName, jobId, "2"));
    }

    @Test
    public void testGetJobOutputFileContentsInvalidJobFileId() throws Exception {
        String jobName = job.getJobName();
        String jobId = job.getJobId();
        String fileId = "999";
        verifyExceptionReturn(new JobFileIdNotFoundException(jobName, jobId, fileId),
                getJobFileContent(jobName, jobId, fileId));
    }

    public static Response getJobFileContent(String jobName, String jobId, String fileId) throws Exception {
        return RestAssured.given().header(AUTH_HEADER).when().get(getJobPath(jobName, jobId) + "/files/" + fileId + "/content");
    }
    
    @Test
    public void testGetConcatenatedJobOutputFiles() throws Exception {
        String jobName = job.getJobName();
        String jobId = job.getJobId();
        String expectedContentRegexJESJCL = "        2 //UNIT     EXEC PGM=IEFBR14.*";
        String expectedContentRegexJESYMSG = ".*IEFA111I ATLJ0000 IS USING THE FOLLOWING JOB RELATED SETTINGS:.*";
        String pattern = expectedContentRegexJESMSGLG + expectedContentRegexJESJCL + expectedContentRegexJESYMSG;
        
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        getConcatenatedJobFiles(jobName, jobId).then().statusCode(HttpStatus.SC_OK).body("content",
                MatchesPattern.matchesPattern(regex));
    }
    
    public static Response getConcatenatedJobFiles(String jobName, String jobId) throws Exception {
        return RestAssured.given().header(AUTH_HEADER).when().get(getJobPath(jobName, jobId) + "/files/content");
    }
}