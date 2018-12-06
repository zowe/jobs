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

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.Assert.assertEquals;

public class JclStringValidatorTest {

    private static Validator validator;

    @BeforeClass
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void null_jcl_string_should_fail() {
        testValidatorFailed(null, "JCL string can't be empty");
    }

    @Test
    public void empty_jcl_string_should_fail() {
        testValidatorFailed(null, "JCL string can't be empty");
    }

    @Test
    public void whitespace_jcl_string_should_fail() {
        testValidatorFailed(" \t", "JCL string can't be empty");
    }

    private void testValidatorFailed(String jclValue, String expectedMessage) {
        Set<ConstraintViolation<Object>> violations = validator.validate(new SubmitJobStringRequest(jclValue));
        assertEquals(1, violations.size());
        assertEquals(expectedMessage, violations.iterator().next().getMessage());
    }

}
