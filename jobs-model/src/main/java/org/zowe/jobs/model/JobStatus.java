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

@Schema
public enum JobStatus {
    ACTIVE, OUTPUT, INPUT, ALL;

    public boolean matches(JobStatus otherStatus) {
        return this == ALL || otherStatus == ALL || this == otherStatus;
    }
}
