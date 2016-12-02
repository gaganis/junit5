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

import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.engine.execution.AfterEachMethodAdapter;
import org.junit.jupiter.engine.execution.BeforeEachMethodAdapter;
import org.junit.jupiter.engine.execution.ConditionEvaluator;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.TestMethodExecutionStrategy;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.util.ExceptionUtils;

class SingleInvocationExecutionStrategy implements TestMethodExecutionStrategy {

	private static final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();

	private final ThrowingConsumer<JupiterEngineExecutionContext> testMethodCaller;

	SingleInvocationExecutionStrategy(ThrowingConsumer<JupiterEngineExecutionContext> testMethodCaller) {
		this.testMethodCaller = testMethodCaller;
	}

	@Override
	public ConditionEvaluationResult evaluateConditions(JupiterEngineExecutionContext context) {
		return conditionEvaluator.evaluateForTest(context.getExtensionRegistry(), context.getConfigurationParameters(),
			(TestExtensionContext) context.getExtensionContext());
	}

	@Override
	public void execute(JupiterEngineExecutionContext context) {
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		// @formatter:off
		invokeBeforeEachCallbacks(context);
		if (throwableCollector.isEmpty()) {
			invokeBeforeEachMethods(context);
			if (throwableCollector.isEmpty()) {
				invokeBeforeTestExecutionCallbacks(context);
				if (throwableCollector.isEmpty()) {
					invokeTestMethod(context);
				}
				invokeAfterTestExecutionCallbacks(context);
			}
			invokeAfterEachMethods(context);
		}
		invokeAfterEachCallbacks(context);
		// @formatter:on

		throwableCollector.assertEmpty();
	}

	private void invokeBeforeEachCallbacks(JupiterEngineExecutionContext context) {
		invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(context,
			((extensionContext, callback) -> () -> callback.beforeEach(extensionContext)), BeforeEachCallback.class);
	}

	private void invokeBeforeEachMethods(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(context,
			((extensionContext, adapter) -> () -> adapter.invokeBeforeEachMethod(extensionContext, registry)),
			BeforeEachMethodAdapter.class);
	}

	private void invokeBeforeTestExecutionCallbacks(JupiterEngineExecutionContext context) {
		invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(context,
			((extensionContext, callback) -> () -> callback.beforeTestExecution(extensionContext)),
			BeforeTestExecutionCallback.class);
	}

	private <T extends Extension> void invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(
			JupiterEngineExecutionContext context, BiFunction<TestExtensionContext, T, Executable> generator,
			Class<T> type) {

		ExtensionRegistry registry = context.getExtensionRegistry();
		TestExtensionContext testExtensionContext = (TestExtensionContext) context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		for (T callback : registry.getExtensions(type)) {
			Executable executable = generator.apply(testExtensionContext, callback);
			throwableCollector.execute(executable);
			if (throwableCollector.isNotEmpty()) {
				break;
			}
		}
	}

	private void invokeTestMethod(JupiterEngineExecutionContext context) {
		context.getThrowableCollector().execute(() -> {
			try {
				testMethodCaller.accept(context);
			}
			catch (Throwable throwable) {
				invokeTestExecutionExceptionHandlers(context, throwable);
			}
		});
	}

	private void invokeTestExecutionExceptionHandlers(JupiterEngineExecutionContext context, Throwable ex) {
		List<TestExecutionExceptionHandler> exceptionHandlers = context.getExtensionRegistry().getExtensions(
			TestExecutionExceptionHandler.class);
		TestExtensionContext testExtensionContext = (TestExtensionContext) context.getExtensionContext();
		invokeTestExecutionExceptionHandlers(ex, exceptionHandlers, testExtensionContext);
	}

	private void invokeTestExecutionExceptionHandlers(Throwable ex, List<TestExecutionExceptionHandler> handlers,
			TestExtensionContext context) {

		// No handlers left?
		if (handlers.isEmpty()) {
			ExceptionUtils.throwAsUncheckedException(ex);
		}

		try {
			// Invoke next available handler
			handlers.remove(0).handleTestExecutionException(context, ex);
		}
		catch (Throwable t) {
			invokeTestExecutionExceptionHandlers(t, handlers, context);
		}
	}

	private void invokeAfterTestExecutionCallbacks(JupiterEngineExecutionContext context) {
		invokeAllAfterMethodsOrCallbacks(context,
			((extensionContext, callback) -> () -> callback.afterTestExecution(extensionContext)),
			AfterTestExecutionCallback.class);
	}

	private void invokeAfterEachMethods(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		invokeAllAfterMethodsOrCallbacks(context,
			((extensionContext, adapter) -> () -> adapter.invokeAfterEachMethod(extensionContext, registry)),
			AfterEachMethodAdapter.class);
	}

	private void invokeAfterEachCallbacks(JupiterEngineExecutionContext context) {
		invokeAllAfterMethodsOrCallbacks(context,
			((extensionContext, callback) -> () -> callback.afterEach(extensionContext)), AfterEachCallback.class);
	}

	private <T extends Extension> void invokeAllAfterMethodsOrCallbacks(JupiterEngineExecutionContext context,
			BiFunction<TestExtensionContext, T, Executable> generator, Class<T> type) {

		ExtensionRegistry registry = context.getExtensionRegistry();
		TestExtensionContext testExtensionContext = (TestExtensionContext) context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		registry.getReversedExtensions(type).forEach(callback -> {
			Executable executable = generator.apply(testExtensionContext, callback);
			throwableCollector.execute(executable);
		});
	}
}
