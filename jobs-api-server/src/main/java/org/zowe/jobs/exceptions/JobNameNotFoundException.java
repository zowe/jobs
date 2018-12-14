/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2018
 */
package org.zowe.jobs.exceptions;

import org.springframework.http.HttpStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;

public class JobNameNotFoundException extends ZoweApiRestException {

    /**
     * 
     */
    private static final long serialVersionUID = 6936887858320598970L;

    // TODO - bad request, or not found?
    public JobNameNotFoundException(String jobName, String jobId) {
        super(HttpStatus.NOT_FOUND, "No job with name ''{0}'' and id ''{1}'' was found", jobName, jobId);
    }

}
