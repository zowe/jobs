/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */
package org.zowe.jobs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitJobStringRequest {

    @Schema(description = "The jcl to be submitted, with \\n for new lines", required = true, example = "//TESTJOBX JOB (),MSGCLASS=H\n// EXEC PGM=IEFBR14")
    @ValidJclString
    private String jcl;
}
