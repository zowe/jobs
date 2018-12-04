/*
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
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import java.util.HashMap;

//TODO LATER - fix to use RestAssured
public class JobsGetIntegrationTest extends AbstractJobsIntegrationTest {

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
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobs() {
        System.out.println("> testGetJobs()");

        String relativeURI = "jobs";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_regex.txt";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
    }

    /**
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobsWithUnlikelyPrefix() {
        System.out.println("> testGetJobsWithUnlikelyPrefix()");

        String relativeURI = "jobs?prefix=12345678";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_unlikelyPrefix.json";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode);
    }

    /**
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobsWithInvalidPrefix() {
        System.out.println("> testGetJobsWithInvalidPrefix()");

        String relativeURI = "jobs?prefix=123456789";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_invalidPrefix.json";
        int expectedReturnCode = HttpStatus.SC_BAD_REQUEST;

        HashMap<String, String> substitutionVars = new HashMap<String, String>();
        substitutionVars.put("ATLAS.USERNAME", USER.toUpperCase());

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode,
                substitutionVars, false);
    }

    /**
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobsWithUnlikelyOwner() {
        System.out.println("> testGetJobsWithUnlikelyOwner()");

        String relativeURI = "jobs?owner=12345678";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_unlikelyPrefix.json";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode);
    }

    /**
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobsWithInvalidOwner() {
        System.out.println("> testGetJobsWithInvalidOwner()");

        String relativeURI = "jobs?owner=123456789";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_invalidOwner.json";
        int expectedReturnCode = HttpStatus.SC_BAD_REQUEST;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, false);
    }

    /**
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobsWithOwnerAndPrefix() {
        System.out.println("> testGetJobsWithOwnerAndPrefix()");

        String relativeURI = "jobs?owner=" + USER + "&prefix=*";
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_regex.txt";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode, null, true);
    }

    /**
     * GET /Atlas/jobs
     */
    @Test
    public void testGetJobsWithCurrentUserAsOwnerAndSpecificPrefix() throws Exception {
        System.out.println("> testGetJobsWithSpecificOwnerAndPrefix()");

        String relativeURI = "jobs?prefix=" + job.getJobName();
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_specificPrefix_regex.txt";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode,
                getSubstitutionVars(job), true);
    }

    /**
     * GET /Atlas/jobs
     *
     * @throws Exception
     */
    @Test
    public void testGetJobsWithCurrentUserAsOwnerSpecificPrefixAndStatus() throws Exception {
        System.out.println("> testGetJobsWithSpecificOwnerAndPrefix()");

        String relativeURI = "jobs?status=OUTPUT&prefix=" + job.getJobName();
        String httpMethodType = HttpGet.METHOD_NAME;
        String expectedResultFilePath = "expectedResults/Jobs/Jobs_specificPrefix_regex.txt";
        int expectedReturnCode = HttpStatus.SC_OK;

        runAndVerifyHTTPRequest(relativeURI, httpMethodType, expectedResultFilePath, expectedReturnCode,
                getSubstitutionVars(job), true);
    }
}