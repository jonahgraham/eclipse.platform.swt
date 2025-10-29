/*******************************************************************************
 * Copyright (c) 2025 Kichwa Coders Canada, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.swt.tests;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CheckForLeaksEmpty
		implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
	}

	@Override
	public void afterAll(ExtensionContext context) {
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		Method method = context.getTestMethod().get();
		System.out.println(method);
		Annotation[] annotations = method.getAnnotations();
		System.out.println(Arrays.asList(annotations));
		AllowLeaks annotation = method.getAnnotation(AllowLeaks.class);
		System.out.println(annotation);
	}

	@Override
	public void afterEach(ExtensionContext context) {
	}

}
