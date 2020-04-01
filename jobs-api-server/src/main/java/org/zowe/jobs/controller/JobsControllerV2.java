/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2019
 */
package org.zowe.jobs.controller;

import io.swagger.annotations.Api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.jobs.services.JobsService;

@RestController
@RequestMapping("/api/v2/jobs")
@Api(value = "JES Jobs APIs", tags = "JES job APIs")
public class JobsControllerV2 extends AbstractJobsController {

    @Autowired 
    @Qualifier("ZosmfJobsServiceV2")
    private JobsService jobsService;
    
    public JobsControllerV2() {
        setJobsService(jobsService);
    }
    
}