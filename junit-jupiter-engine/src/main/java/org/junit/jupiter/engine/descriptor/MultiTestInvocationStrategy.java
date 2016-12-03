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

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.engine.descriptor.TestInvocationTestDescriptor.TEST_INVOCATION_SEGMENT_TYPE;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.junit.jupiter.api.extension.TestInvocationContextProvider;
import org.junit.jupiter.api.extension.TestInvocationContextProvider.TestInvocationContext;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.engine.execution.ConditionEvaluator;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.TestInvocationStrategy;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.SingleTestExecutor;

class MultiTestInvocationStrategy implements TestInvocationStrategy {

	private static final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
	private static final SingleTestExecutor singleTestExecutor = new SingleTestExecutor();

	private final AbstractTestDescriptor containerTestDescriptor;
	private final SingleTestInvocationStrategy singleTestInvocationStrategy;
	private final List<TestInvocationContextProvider> testInvocationContextProviders;

	MultiTestInvocationStrategy(AbstractTestDescriptor testDescriptor,
			ThrowingConsumer<JupiterEngineExecutionContext> testMethodCaller,
			List<TestInvocationContextProvider> testInvocationContextProviders) {
		this.containerTestDescriptor = testDescriptor;
		this.singleTestInvocationStrategy = new SingleTestInvocationStrategy(testMethodCaller);
		this.testInvocationContextProviders = testInvocationContextProviders;
	}

	@Override
	public ConditionEvaluationResult evaluateConditions(JupiterEngineExecutionContext context) {
		return conditionEvaluator.evaluateForContainer(context.getExtensionRegistry(),
			context.getConfigurationParameters(), (ContainerExtensionContext) context.getExtensionContext());
	}

	@Override
	public void execute(JupiterEngineExecutionContext context) {
		AtomicInteger index = new AtomicInteger(0);
		ContainerExtensionContext contextExtensionContext = (ContainerExtensionContext) context.getExtensionContext();
		// @formatter:off
        testInvocationContextProviders.stream()
                .map(provider -> provider.provide(contextExtensionContext))
				.flatMap(iterator -> stream(spliteratorUnknownSize(iterator, ORDERED), false))
                .forEach(invocationContext -> processTestInvocation(context, invocationContext, index.getAndIncrement()));
        // @formatter:on
	}

	private void processTestInvocation(JupiterEngineExecutionContext parentContext,
			TestInvocationContext invocationContext, int index) {
		TestInvocationTestDescriptor testDescriptor = buildTestDescriptor(parentContext, invocationContext, index);
		JupiterEngineExecutionContext executionContext = buildExecutionContext(testDescriptor, parentContext,
			invocationContext);
		skipOrExecute(testDescriptor, executionContext);
	}

	private JupiterEngineExecutionContext buildExecutionContext(TestInvocationTestDescriptor testDescriptor,
			JupiterEngineExecutionContext parentContext, TestInvocationContext invocationContext) {
		Object testInstance = getTestInstance(parentContext);
		ThrowableCollector throwableCollector = new ThrowableCollector();
		TestExtensionContext testExtensionContext = new TestInvocationExtensionContext(
			parentContext.getExtensionContext(), parentContext.getExecutionListener(), testDescriptor, testInstance,
			throwableCollector);
		ExtensionRegistry registry = ExtensionRegistry.createRegistryFromInstances(parentContext.getExtensionRegistry(),
			invocationContext.getExtensions());
		// @formatter:off
        return parentContext.extend()
                .withExtensionRegistry(registry)
                .withThrowableCollector(throwableCollector)
                .withExtensionContext(testExtensionContext)
                .build();
        // @formatter:on
	}

	private TestInvocationTestDescriptor buildTestDescriptor(JupiterEngineExecutionContext parentContext,
			TestInvocationContext invocationContext, int index) {
		UniqueId uniqueId = containerTestDescriptor.getUniqueId().append(TEST_INVOCATION_SEGMENT_TYPE, "#" + index);
		String displayName = invocationContext.getDisplayName(
			(ContainerExtensionContext) parentContext.getExtensionContext(), index);
		TestInvocationTestDescriptor testDescriptor = new TestInvocationTestDescriptor(uniqueId, displayName,
			containerTestDescriptor.getSource());
		containerTestDescriptor.addChild(testDescriptor);
		return testDescriptor;
	}

	private void skipOrExecute(TestDescriptor descriptor, JupiterEngineExecutionContext context) {
		EngineExecutionListener listener = context.getExecutionListener();
		listener.dynamicTestRegistered(descriptor);

		ConditionEvaluationResult conditionEvaluationResult = conditionEvaluator.evaluateForTest(
			context.getExtensionRegistry(), context.getConfigurationParameters(),
			(TestExtensionContext) context.getExtensionContext());

		if (conditionEvaluationResult.isDisabled()) {
			listener.executionSkipped(descriptor, conditionEvaluationResult.getReason().orElse("<unknown>"));
		}
		else {
			listener.executionStarted(descriptor);
			TestExecutionResult result = singleTestExecutor.executeSafely(
				() -> singleTestInvocationStrategy.execute(context));
			listener.executionFinished(descriptor, result);
		}
	}

	private Object getTestInstance(JupiterEngineExecutionContext context) {
		try {
			return context.getTestInstanceProvider().getTestInstance();
		}
		catch (Exception ex) {
			throw ExceptionUtils.throwAsUncheckedException(ex);
		}
	}

}