/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2020
 */

package org.zowe.jobs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.jobs.services.JobsService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v2/jobs")
@Tag(name = "JES Jobs APIs V2")
public class JobsControllerV2 extends AbstractJobsController {

    @Autowired
    @Qualifier("ZosmfJobsServiceV2")
    private JobsService jobsService;

    @Autowired
    private HttpServletRequest request;

    @Override
    public JobsService getJobsService() {
        if (request != null) {
            jobsService.setRequest(request);
        }
        return jobsService;
    }
}