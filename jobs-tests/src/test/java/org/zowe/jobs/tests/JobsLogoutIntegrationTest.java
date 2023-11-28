/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019, 2020
 */

package org.zowe.jobs.tests;

import io.restassured.RestAssured;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

import static org.junit.Assume.assumeTrue;

public class JobsLogoutIntegrationTest extends AbstractJobsIntegrationTest {

    private static Job job;
    public static String path;
    
    @Before
    public void checkVersionBeforeRunningTest() throws Exception {
        assumeTrue(System.getProperty("test.version").equals("1"));        
    }

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        path = job.getJobName() + "/" + job.getJobId();
    }

    @AfterClass
    public static void purgeJob() throws Exception {
        deleteJob(job);
    }

    @Test
    public void testGetJobsWithoutAuth() throws Exception {
        RestAssured.given().when().auth().none().get(path)
            .then().statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void testLogoutWithGetJobs() throws Exception {
        String cookie = RestAssured.given().header(AUTH_HEADER).when().get(path).getCookie("JSESSIONID");

        RestAssured.given().when().auth().none().cookie("JSESSIONID", cookie).get(path)
            .then().statusCode(HttpStatus.SC_OK);

        RestAssured.given().when().auth().none().cookie("JSESSIONID", cookie).post("/logout")
            .then().statusCode(HttpStatus.SC_NO_CONTENT);

        RestAssured.given().when().auth().none().cookie("JSESSIONID", cookie).get(path)
            .then().statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}