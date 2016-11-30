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

import java.util.function.Consumer;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.TestExtensionContext;
import org.junit.jupiter.engine.execution.ConditionEvaluator;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.TestMethodExecutionStrategy;

class SingleInvocationExecutionStrategy implements TestMethodExecutionStrategy {

	private static final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();

	@Override
	public ConditionEvaluationResult evaluateConditions(JupiterEngineExecutionContext context) {
		return conditionEvaluator.evaluateForTest(context.getExtensionRegistry(), context.getConfigurationParameters(),
			(TestExtensionContext) context.getExtensionContext());
	}

	@Override
	public void execute(JupiterEngineExecutionContext context, Consumer<JupiterEngineExecutionContext> executor) {
		executor.accept(context);
	}
}
