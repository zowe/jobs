/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2019
 */
package org.zowe.jobs.services;

import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

public interface JobsServiceV1 {

    ItemsWrapper<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException;

    Job getJob(String jobName, String jobId);

    void purgeJob(String jobName, String jobId);
    
    void modifyJob(String jobName, String jobId, String command);

    Job submitJobString(String jclString);

    Job submitJobFile(String file);

    ItemsWrapper<JobFile> getJobFiles(String jobName, String jobId);

    JobFileContent getJobFileContent(String jobName, String jobId, String fileId);

    JobFileContent getJobJcl(String jobName, String jobId);

}
