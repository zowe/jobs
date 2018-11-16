/**
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.zowe.api.common.exceptions.ZoweApiRestException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends ZoweApiRestException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2568539995597255984L;

	public BadRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}

}