/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */
package org.zowe.jobs.services;

import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

import java.util.List;

public interface JobsService {

    List<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException;

    Job getJob(String jobName, String jobId);

    void purgeJob(String jobName, String jobId);

    Job submitJobString(String jclString);

    Job submitJobFile(String file);

    List<JobFile> getJobFiles(String jobName, String jobId);

    JobFileContent getJobFileContent(String jobName, String jobId, String fileId);
//
//    OutputFile getJobJcl(String jobName, String jobId);

}
