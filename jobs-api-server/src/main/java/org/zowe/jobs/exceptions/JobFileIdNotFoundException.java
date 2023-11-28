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

public class JobFileIdNotFoundException extends ZoweApiRestException {

    /**
     * 
     */
    private static final long serialVersionUID = 4247535068979390540L;

    public JobFileIdNotFoundException(String jobName, String jobId, String fileId) {
        super(HttpStatus.NOT_FOUND, "No spool file with id ''{0}'' was found for job {1}({2})", fileId, jobName, jobId);
    }

}
