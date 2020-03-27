/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */
package org.zowe.jobs.services.zosmf.v1;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.zosmf.services.AbstractZosmfRequestRunnerV1;
import org.zowe.jobs.exceptions.JobIdNotFoundException;
import org.zowe.jobs.exceptions.JobNameNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

public abstract class AbstractZosmfJobsRequestRunner<T> extends AbstractZosmfRequestRunnerV1<T> {

    ZoweApiRestException createJobNotFoundExceptions(JsonObject jsonResponse, int statusCode, String jobName,
            String jobId) {
        if (jsonResponse.has("message")) {
            String zosmfMessage = jsonResponse.get("message").getAsString();
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                if (String.format("No job found for reference: '%s(%s)'", jobName, jobId).equals(zosmfMessage)) {
                    return new JobNameNotFoundException(jobName, jobId);
                }
            } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                if (String.format("Failed to lookup job %s(%s)", jobName, jobId).equals(zosmfMessage)) {
                    return new JobIdNotFoundException(jobName, jobId);
                }
            }
        }
        return null;
    }

    Job getJobFromJson(JsonObject returned) {
        return Job.builder().jobId(returned.get("jobid").getAsString()) //$NON-NLS-1$
            .jobName(returned.get("jobname").getAsString()) //$NON-NLS-1$
            .owner(returned.get("owner").getAsString()) //$NON-NLS-1$
            .type(returned.get("type").getAsString()) //$NON-NLS-1$
            .status(JobStatus.valueOf(returned.get("status").getAsString())) //$NON-NLS-1$
            .returnCode(getStringOrNull(returned, "retcode")) //$NON-NLS-1$
            .subsystem(returned.get("subsystem").getAsString()) //$NON-NLS-1$
            .executionClass(returned.get("class").getAsString()) //$NON-NLS-1$
            .phaseName(returned.get("phase-name").getAsString()) //$NON-NLS-1$
            .build();
    }
}