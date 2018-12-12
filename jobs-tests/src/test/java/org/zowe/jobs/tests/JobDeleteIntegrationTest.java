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
import org.junit.Test;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

public class JobDeleteIntegrationTest extends AbstractJobsIntegrationTest {

    /**
     * DELETE /api/v1/jobs
     */
    @Test
    public void testDeleteJob() throws Exception {
        Job job = submitJobAndPoll(JOB_IEFBR14, JobStatus.OUTPUT);
        purgeJob(job).shouldHaveStatusNoContent();
    }

    @Test
    public void testDeleteJobInvalidJob() throws Exception {
        Job job = Job.builder().jobName("DUMMYJOB").jobId("JOB00000").build();
        purgeJob(job).shouldHaveStatus(HttpStatus.SC_BAD_REQUEST);
    }
}