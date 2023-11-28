/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018, 2020
 */

package org.zowe.jobs.services;

import lombok.Setter;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.zowe.api.common.exceptions.ZoweApiException;
import org.zowe.api.common.model.ItemsWrapper;
import org.zowe.jobs.model.Job;
import org.zowe.jobs.model.JobFile;
import org.zowe.jobs.model.JobFileContent;
import org.zowe.jobs.model.JobStatus;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Setter
public abstract class JobsService {
    
    private HttpServletRequest request;
    
    public List<Header> getIbmHeadersFromRequest() {
        ArrayList<Header> ibmHeaders = new ArrayList<Header>();
        if (request != null ) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement().toUpperCase();
                if (headerName.contains("X-IBM")) {
                    Header newHeader = new BasicHeader(headerName, request.getHeader(headerName));
                    ibmHeaders.add(newHeader);
                }
            }
        }
        return ibmHeaders;
    }

    public abstract ItemsWrapper<Job> getJobs(String prefix, String owner, JobStatus status) throws ZoweApiException;

    public abstract Job getJob(String jobName, String jobId);

    public abstract void purgeJob(String jobName, String jobId);
    
    public abstract void modifyJob(String jobName, String jobId, String command);

    public abstract Job submitJobString(String jclString);

    public abstract Job submitJobFile(String file);

    public abstract ItemsWrapper<JobFile> getJobFiles(String jobName, String jobId);

    public abstract JobFileContent getJobFileContent(String jobName, String jobId, String fileId);

    public abstract JobFileContent getJobJcl(String jobName, String jobId);

}
