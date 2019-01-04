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

import org.junit.Ignore;
import org.junit.Test;
import org.zowe.api.common.errors.ApiError;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

public class JobDeleteIntegrationTest extends AbstractJobsIntegrationTest {

    @Test
    @Ignore("See todo") // TODO - fix CIM server on river
    public void testDeleteJob() throws Exception {
        Job job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        purgeJob(job).shouldHaveStatusNoContent();
    }

    @Test
    public void testDeleteJobInvalidJob() throws Exception {
        Job job = Job.builder().jobName("DUMMYJOB").jobId("JOB00000").build();
        ApiError expected = ApiError.builder().status(org.springframework.http.HttpStatus.NOT_FOUND)
                .message(String.format("No job with name '%s' and id '%s' was found", job.getJobName(), job.getJobId()))
                .build();
        purgeJob(job).shouldReturnError(expected);
    }
}