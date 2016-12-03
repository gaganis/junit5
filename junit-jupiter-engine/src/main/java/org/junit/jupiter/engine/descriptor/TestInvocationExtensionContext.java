/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.platform.commons.meta.API.Usage.Internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.EngineExecutionListener;

/**
 * @since 5.0
 */
@API(Internal)
final class TestInvocationExtensionContext extends AbstractExtensionContext<TestInvocationTestDescriptor>
		implements TestExtensionContext {

	private final Object testInstance;
	private final ThrowableCollector throwableCollector;

	TestInvocationExtensionContext(ExtensionContext parent, EngineExecutionListener engineExecutionListener,
			TestInvocationTestDescriptor testDescriptor, Object testInstance, ThrowableCollector throwableCollector) {
		super(parent, engineExecutionListener, testDescriptor);
		this.testInstance = testInstance;
		this.throwableCollector = throwableCollector;
	}

	@Override
	public String getUniqueId() {
		return getTestDescriptor().getUniqueId().toString();
	}

	@Override
	public String getDisplayName() {
		return getTestDescriptor().getDisplayName();
	}

	@Override
	public Optional<AnnotatedElement> getElement() {
		return getMethodTestDescriptor().map(MethodTestDescriptor::getTestMethod);
	}

	@Override
	public Optional<Class<?>> getTestClass() {
		return getMethodTestDescriptor().map(MethodTestDescriptor::getTestClass);
	}

	@Override
	public Optional<Method> getTestMethod() {
		return getMethodTestDescriptor().map(MethodTestDescriptor::getTestMethod);
	}

	@Override
	public Object getTestInstance() {
		return this.testInstance;
	}

	@Override
	public Optional<Throwable> getTestException() {
		return Optional.ofNullable(this.throwableCollector.getThrowable());
	}

	private Optional<MethodTestDescriptor> getMethodTestDescriptor() {
		// @formatter:off
		return getTestDescriptor().getParent()
				.filter(MethodTestDescriptor.class::isInstance)
				.map(MethodTestDescriptor.class::cast);
		// @formatter:on
	}
}