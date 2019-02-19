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

import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

public abstract class AbstractZosmfJobsRequestRunnerTest extends AbstractZosmfRequestRunnerTest {

    static Job createJob(String id, String jobName, String owner, String type, JobStatus status, String phase,
            String returnCode) {
        return Job.builder().jobId(id) // $NON-NLS-1$
            .jobName(jobName) // $NON-NLS-1$
            .owner(owner) // $NON-NLS-1$
            .type(type) // $NON-NLS-1$
            .status(status) // $NON-NLS-1$
            .subsystem("JES2") //$NON-NLS-1$
            .executionClass(type) // $NON-NLS-1$
            .phaseName(phase) // $NON-NLS-1$
            .returnCode(returnCode) // $NON-NLS-1$
            .build();
    }
}
