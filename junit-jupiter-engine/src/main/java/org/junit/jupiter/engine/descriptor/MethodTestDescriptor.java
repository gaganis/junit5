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

import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.meta.API.Usage.Internal;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.junit.jupiter.api.extension.TestInvocationContextProvider;
import org.junit.jupiter.engine.execution.ExecutableInvoker;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.TestInvocationStrategy;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * {@link TestDescriptor} for tests based on Java methods.
 *
 * <h3>Default Display Names</h3>
 *
 * <p>The default display name for a test method is the name of the method
 * concatenated with a comma-separated list of parameter types in parentheses.
 * The names of parameter types are retrieved using {@link Class#getSimpleName()}.
 * For example, the default display name for the following test method is
 * {@code testUser(TestInfo, User)}.
 *
 * <pre style="code">
 *   {@literal @}Test
 *   void testUser(TestInfo testInfo, {@literal @}Mock User user) { ... }
 * </pre>
 *
 * @since 5.0
 */
@API(Internal)
public class MethodTestDescriptor extends JupiterTestDescriptor {

	private static final ExecutableInvoker executableInvoker = new ExecutableInvoker();

	private final Class<?> testClass;
	private final Method testMethod;

	public MethodTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method testMethod) {
		super(uniqueId, determineDisplayName(Preconditions.notNull(testMethod, "Method must not be null"),
			MethodTestDescriptor::generateDefaultDisplayName));

		this.testClass = Preconditions.notNull(testClass, "Class must not be null");
		this.testMethod = testMethod;

		setSource(new MethodSource(testMethod));
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public final Set<TestTag> getTags() {
		Set<TestTag> methodTags = getTags(getTestMethod());
		getParent().ifPresent(parentDescriptor -> methodTags.addAll(parentDescriptor.getTags()));
		return methodTags;
	}

	public final Class<?> getTestClass() {
		return this.testClass;
	}

	public final Method getTestMethod() {
		return this.testMethod;
	}

	@Override
	public boolean isTest() {
		return true;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	protected static String generateDefaultDisplayName(Method testMethod) {
		return String.format("%s(%s)", testMethod.getName(),
			StringUtils.nullSafeToString(Class::getSimpleName, testMethod.getParameterTypes()));
	}

	// --- Node ----------------------------------------------------------------

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) throws Exception {
		ExtensionRegistry registry = populateNewExtensionRegistryFromExtendWith(this.testMethod,
			context.getExtensionRegistry());
		MethodBasedContainerExtensionContext containerExtensionContext = new MethodBasedContainerExtensionContext(
			context.getExtensionContext(), context.getExecutionListener(), this);
		// @formatter:off
		List<TestInvocationContextProvider> testInvocationContextProviders = registry.stream(TestInvocationContextProvider.class)
				.filter(provider -> provider.supports(containerExtensionContext))
				.collect(toList());
		// @formatter:on
		if (testInvocationContextProviders.isEmpty()) {
			return prepareForSingleInvocation(context, registry);
		}
		return prepareForMultipleInvocations(context, registry, containerExtensionContext,
			testInvocationContextProviders);

	}

	private JupiterEngineExecutionContext prepareForSingleInvocation(JupiterEngineExecutionContext context,
			ExtensionRegistry registry) throws Exception {
		Object testInstance = context.getTestInstanceProvider().getTestInstance();
		ThrowableCollector throwableCollector = new ThrowableCollector();
		TestExtensionContext extensionContext = new MethodBasedTestExtensionContext(context.getExtensionContext(),
			context.getExecutionListener(), this, testInstance, throwableCollector);
		// @formatter:off
		return context.extend()
                .withExtensionRegistry(registry)
                .withExtensionContext(extensionContext)
                .withThrowableCollector(throwableCollector)
                .withTestInvocationStrategy(new SingleTestInvocationStrategy(this::invokeTestMethod))
                .build();
		// @formatter:on
	}

	private JupiterEngineExecutionContext prepareForMultipleInvocations(JupiterEngineExecutionContext context,
			ExtensionRegistry registry, ExtensionContext extensionContext,
			List<TestInvocationContextProvider> testInvocationContextProviders) {
		MultiTestInvocationStrategy testInvocationStrategy = new MultiTestInvocationStrategy(this,
			this::invokeTestMethod, testInvocationContextProviders);
		// @formatter:off
		return context.extend()
				.withExtensionRegistry(registry)
				.withExtensionContext(extensionContext)
				.withTestInvocationStrategy(testInvocationStrategy)
				.build();
		// @formatter:on
	}

	@Override
	public SkipResult shouldBeSkipped(JupiterEngineExecutionContext context) throws Exception {
		TestInvocationStrategy executionStrategy = context.getTestMethodExecutionStrategy();
		ConditionEvaluationResult evaluationResult = executionStrategy.evaluateConditions(context);
		if (evaluationResult.isDisabled()) {
			return SkipResult.skip(evaluationResult.getReason().orElse("<unknown>"));
		}
		return SkipResult.doNotSkip();
	}

	@Override
	public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context) throws Exception {
		context.getTestMethodExecutionStrategy().execute(context);
		return context;
	}

	protected void invokeTestMethod(JupiterEngineExecutionContext context) {
		TestExtensionContext testExtensionContext = (TestExtensionContext) context.getExtensionContext();
		Method method = getTestMethod();
		Object instance = testExtensionContext.getTestInstance();
		executableInvoker.invoke(method, instance, testExtensionContext, context.getExtensionRegistry());
	}
}
