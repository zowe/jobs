/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2020
 */

package org.zowe.jobs.services.zosmf;

import lombok.extern.slf4j.Slf4j;

import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.jobs.exceptions.JobFileIdNotFoundException;
import org.zowe.jobs.exceptions.JobJesjclNotFoundException;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;
import org.zowe.jobs.services.JobsService;

@Slf4j
public abstract class AbstractZosmfJobsService extends JobsService {
    
    abstract ZosmfConnector getZosmfConnector();

    public ItemsWrapper<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException {
        GetJobsZosmfRequestRunner runner = new GetJobsZosmfRequestRunner(prefix, owner, status, getIbmHeadersFromRequest());
        return runner.run(getZosmfConnector());
    }

    public Job getJob(String jobName, String jobId) {
        GetJobZosmfRequestRunner runner = new GetJobZosmfRequestRunner(jobName, jobId, getIbmHeadersFromRequest());
        return runner.run(getZosmfConnector());
    }

    public Job submitJobString(String jcl) {
        SubmitJobStringZosmfRequestRunner runner = new SubmitJobStringZosmfRequestRunner(jcl, getIbmHeadersFromRequest());
        return runner.run(getZosmfConnector());
    }

    public Job submitJobFile(String fileName) {
        SubmitJobFileZosmfRequestRunner runner = new SubmitJobFileZosmfRequestRunner(fileName, getIbmHeadersFromRequest());
        return runner.run(getZosmfConnector());
    }

    public void purgeJob(String jobName, String jobId) {
        PurgeJobZosmfRequestRunner runner = new PurgeJobZosmfRequestRunner(jobName, jobId, getIbmHeadersFromRequest());
        runner.run(getZosmfConnector());
    }
    
    public void modifyJob(String jobName, String jobId, String command) {
        ModifyJobZosmfRequestRunner runner = new ModifyJobZosmfRequestRunner(jobName, jobId, command, getIbmHeadersFromRequest());
        runner.run(getZosmfConnector());
    }

    public ItemsWrapper<JobFile> getJobFiles(String jobName, String jobId) {
        GetJobFilesZosmfRequestRunner runner = new GetJobFilesZosmfRequestRunner(jobName, jobId, getIbmHeadersFromRequest());
        return runner.run(getZosmfConnector());
    }

    public JobFileContent getJobFileContent(String jobName, String jobId, String fileId) {
        GetJobFileContentZosmfRequestRunner runner = new GetJobFileContentZosmfRequestRunner(jobName, jobId, fileId, getIbmHeadersFromRequest());
        return runner.run(getZosmfConnector());
    }

    public JobFileContent getJobJcl(String jobName, String jobId) {
        try {
            return getJobFileContent(jobName, jobId, "3");
        } catch (JobFileIdNotFoundException e) {
            log.error("getJobJcl", e);
            throw new JobJesjclNotFoundException(jobName, jobId);
        }
    }
}
