/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */

package org.zowe.jobs.services;

import java.util.List;

import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobStatus;

public interface JobsService {

	List<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException;

//	Job getJob(String jobName, String jobId);
//
//	void purgeJob(String jobName, String jobId);
//
//	Job submitJobFile(String file);

	Job submitJobString(String jcl);
//
//	List<JobFile> getJobFiles(String jobName, String jobId);
//
//	OutputFile getJobFileRecordsByRange(String jobName, String jobId, String fileId, Integer start, Integer end);
//
//	OutputFile getJobJcl(String jobName, String jobId);

}
