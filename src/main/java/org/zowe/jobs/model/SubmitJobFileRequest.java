/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */

package org.zowe.jobs.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitJobFileRequest {

	@ApiModelProperty(value = "The data set, or z/OS unix file to submit in form: in the form: 'ATLAS.TEST.JCL(TSTJ0001)' for a data set, or /u/myjobs/job1 for z/OS unix file", dataType = "string", required = true, example = "'ATLAS.TEST.JCL(TSTJ0001)'")
	private String file;
}
