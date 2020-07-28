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

import java.util.List;

import org.apache.http.Header;
import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

public interface JobsService {

    ItemsWrapper<Job> getJobs(List<Header> headers, String prefix, String owner, JobStatus status) throws ZoweApiException;

    Job getJob(List<Header> headers, String jobName, String jobId);

    void purgeJob(List<Header> headers, String jobName, String jobId);
    
    void modifyJob(List<Header> headers, String jobName, String jobId, String command);

    Job submitJobString(List<Header> headers, String jclString);

    Job submitJobFile(List<Header> headers, String file);

    ItemsWrapper<JobFile> getJobFiles(List<Header> headers, String jobName, String jobId);

    JobFileContent getJobFileContent(List<Header> headers, String jobName, String jobId, String fileId);

    JobFileContent getJobJcl(List<Header> headers, String jobName, String jobId);

}
