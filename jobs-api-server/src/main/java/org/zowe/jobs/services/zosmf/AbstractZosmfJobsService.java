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

import java.util.List;

import org.apache.http.Header;
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
public abstract class AbstractZosmfJobsService implements JobsService {
    
    abstract ZosmfConnector getZosmfConnector();

    @Override
    public ItemsWrapper<Job> getJobs(List<Header> headers, String prefix, String owner, JobStatus status) throws ZoweApiException {
        GetJobsZosmfRequestRunner runner = new GetJobsZosmfRequestRunner(headers, prefix, owner, status);
        return runner.run(getZosmfConnector());
    }

    @Override
    public Job getJob(List<Header> headers, String jobName, String jobId) {
        GetJobZosmfRequestRunner runner = new GetJobZosmfRequestRunner(headers, jobName, jobId);
        return runner.run(getZosmfConnector());
    }

    @Override
    public Job submitJobString(List<Header> headers, String jcl) {
        SubmitJobStringZosmfRequestRunner runner = new SubmitJobStringZosmfRequestRunner(headers, jcl);
        return runner.run(getZosmfConnector());
    }

    @Override
    public Job submitJobFile(List<Header> headers, String fileName) {
        SubmitJobFileZosmfRequestRunner runner = new SubmitJobFileZosmfRequestRunner(headers, fileName);
        return runner.run(getZosmfConnector());
    }

    @Override
    public void purgeJob(List<Header> headers, String jobName, String jobId) {
        PurgeJobZosmfRequestRunner runner = new PurgeJobZosmfRequestRunner(headers, jobName, jobId);
        runner.run(getZosmfConnector());
    }
    
    @Override
    public void modifyJob(List<Header> headers, String jobName, String jobId, String command) {
        ModifyJobZosmfRequestRunner runner = new ModifyJobZosmfRequestRunner(headers, jobName, jobId, command);
        runner.run(getZosmfConnector());
    }

    @Override
    public ItemsWrapper<JobFile> getJobFiles(List<Header> headers, String jobName, String jobId) {
        GetJobFilesZosmfRequestRunner runner = new GetJobFilesZosmfRequestRunner(headers, jobName, jobId);
        return runner.run(getZosmfConnector());
    }

    @Override
    public JobFileContent getJobFileContent(List<Header> headers, String jobName, String jobId, String fileId) {
        GetJobFileContentZosmfRequestRunner runner = new GetJobFileContentZosmfRequestRunner(headers, jobName, jobId, fileId);
        return runner.run(getZosmfConnector());
    }

    @Override
    public JobFileContent getJobJcl(List<Header> headers, String jobName, String jobId) {
        try {
            return getJobFileContent(headers, jobName, jobId, "3");
        } catch (JobFileIdNotFoundException e) {
            log.error("getJobJcl", e);
            throw new JobJesjclNotFoundException(jobName, jobId);
        }
    }
}
