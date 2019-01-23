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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;

import java.nio.file.Files;
import java.nio.file.Paths;

public class JobGetByIdTest extends AbstractJobsIntegrationTest {

    private static Job job;
    private static Job jobWithHash;

    // TODO MAYBE - Junit 5 nested test for job hash

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14);
        String jcl = new String(Files.readAllBytes(Paths.get("testFiles/" + JOB_IEFBR14)));
        jcl = jcl.replace("ATLJ0000", "JOB#HASH");
        jobWithHash = submitJobJclString(jcl).getEntityAs(Job.class);

    }

    @AfterClass
    public static void purgeJob() throws Exception {
        purgeJob(job);
        purgeJob(jobWithHash);
    }

    @Test
    public void testGetJobByNameAndId() throws Exception {
        Job actualJob = getJob(job).shouldHaveStatusOk().getEntityAs(Job.class);
        verifyJobIsAsExpected("expectedResults/Jobs/JobsResponse.json", actualJob);
    }

    @Test
    public void testGetJobByNameWithHashAndId() throws Exception {
        Job actualJob = getJob(jobWithHash).shouldHaveStatusOk().getEntityAs(Job.class);
        verifyJobIsAsExpected("expectedResults/Jobs/JobsResponse.json", actualJob);
    }

    @Test
    public void testGetJobByNameAndNonexistingId() throws Exception {
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
            .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), "z000000")).build();
        getJob(job.getJobName(), "z000000").shouldReturnError(expected);
    }
}