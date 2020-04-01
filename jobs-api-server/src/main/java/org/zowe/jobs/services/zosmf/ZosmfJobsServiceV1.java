/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2020
 */
package org.zowe.jobs.services.zosmf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.api.common.connectors.zosmf.ZosmfConnectorLtpaAuth;
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
@Service("ZosmfJobsServiceV1")
public class ZosmfJobsServiceV1 implements JobsService {

    @Autowired
    ZosmfConnectorLtpaAuth zosmfConnector;

    // TODO LATER - review error handling, serviceability https://github.com/zowe/jobs/issues/18
    // use the zomsf error categories to work out errors?
    // https://www.ibm.com/support/knowledgecenter/SSLTBW_2.3.0/com.ibm.zos.v2r3.izua700/IZUHPINFO_API_Error_Categories.htm?

    @Override
    public ItemsWrapper<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException {
        GetJobsZosmfRequestRunner runner = new GetJobsZosmfRequestRunner(prefix, owner, status);
        return runner.run(zosmfConnector);
    }

    @Override
    public Job getJob(String jobName, String jobId) {
        GetJobZosmfRequestRunner runner = new GetJobZosmfRequestRunner(jobName, jobId);
        return runner.run(zosmfConnector);
    }

    @Override
    public Job submitJobString(String jcl) {
        SubmitJobStringZosmfRequestRunner runner = new SubmitJobStringZosmfRequestRunner(jcl);
        return runner.run(zosmfConnector);
    }

    @Override
    public Job submitJobFile(String fileName) {
        SubmitJobFileZosmfRequestRunner runner = new SubmitJobFileZosmfRequestRunner(fileName);
        return runner.run(zosmfConnector);
    }

    @Override
    public void purgeJob(String jobName, String jobId) {
        PurgeJobZosmfRequestRunner runner = new PurgeJobZosmfRequestRunner(jobName, jobId);
        runner.run(zosmfConnector);
    }
    
    @Override
    public void modifyJob(String jobName, String jobId, String command) {
        ModifyJobZosmfRequestRunner runner = new ModifyJobZosmfRequestRunner(jobName, jobId, command);
        runner.run(zosmfConnector);
    }

    @Override
    public ItemsWrapper<JobFile> getJobFiles(String jobName, String jobId) {
        GetJobFilesZosmfRequestRunner runner = new GetJobFilesZosmfRequestRunner(jobName, jobId);
        return runner.run(zosmfConnector);
    }

    @Override
    public JobFileContent getJobFileContent(String jobName, String jobId, String fileId) {
        GetJobFileContentZosmfRequestRunner runner = new GetJobFileContentZosmfRequestRunner(jobName, jobId, fileId);
        return runner.run(zosmfConnector);
    }

    @Override
    public JobFileContent getJobJcl(String jobName, String jobId) {
        try {
            return getJobFileContent(jobName, jobId, "3");
        } catch (JobFileIdNotFoundException e) {
            log.error("getJobJcl", e);
            throw new JobJesjclNotFoundException(jobName, jobId);
        }
    }
}