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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

public class JclStringValidator implements ConstraintValidator<ValidJclString, String> {

	@Override
	public void initialize(ValidJclString jclString) {
	}

	@Override
	public boolean isValid(String jcl, ConstraintValidatorContext constraintContext) {
		if (!StringUtils.hasText(jcl)) {
			addError(constraintContext, "JCL string can't be empty");
			return false;
		}

		return true;
	}

	private void addError(ConstraintValidatorContext constraintContext, String message) {
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}

}