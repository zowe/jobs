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

public class JobGetByIdTest extends AbstractJobsIntegrationTest {

    private static Job job;

    @BeforeClass
    public static void submitJob() throws Exception {
        job = submitJobAndPoll(JOB_IEFBR14);
    }

    @AfterClass
    public static void purgeJob() throws Exception {
        purgeJob(job);
    }

    /**
     * GET /api/v1/jobs/<jobname>/<jobid>
     */

    @Test
    public void testGetJobByNameAndId() throws Exception {
        Job actualJob = getJob(job).shouldHaveStatusOk().getEntityAs(Job.class);

        verifyJobIsAsExpected("expectedResults/Jobs/JobsResponse.json", actualJob);
    }

    @Test
    public void testGetJobByNameAndNonexistingId() throws Exception {
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), "z000000"))
                .build();
        getJob(job.getJobName(), "z000000").shouldReturnError(expected);
    }
}