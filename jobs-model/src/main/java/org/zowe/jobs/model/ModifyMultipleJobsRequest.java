/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */
package org.zowe.jobs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ModifyMultipleJobsRequest {
    @Schema(description = "The modify command, e.g. cancel, hold, release", required = true, example = "cancel")
    private String command;
    @Schema(description = "The list of jobs to receive the modify command", type = "string", required = true, example = "[{\"jobId\":\"job1234\", \"jobName\":\"TestJob\"}]")
    private ArrayList<SimpleJob> jobs;
}
