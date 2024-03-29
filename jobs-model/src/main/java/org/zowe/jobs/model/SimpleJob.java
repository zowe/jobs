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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleJob {
    @Schema(description = "The name of a job", requiredMode = Schema.RequiredMode.REQUIRED, example = "TESTJOB")
    private String jobName;
    @Schema(description = "The id of a job", requiredMode = Schema.RequiredMode.REQUIRED, example = "JOB00001")
    private String jobId;
}
