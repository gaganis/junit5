/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Collections.emptyIterator;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.joining;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestInvocationContext;
import org.junit.jupiter.api.extension.TestInvocationContextProvider;
import org.junit.platform.commons.JUnitException;

public class SimpleParameterizedTests {

	static class TestCase {
		@Test
		@ExtendWith(SimpleParameterizedExtension.class)
		void test(@StringValues({ "foo", "bar" }) String parameter, TestInfo testInfo, TestReporter reporter) {
			reporter.publishEntry(testInfo.getDisplayName(), parameter);
		}
	}

	static class SimpleParameterizedExtension implements TestInvocationContextProvider {
		@Override
		public Iterator<TestInvocationContext> provideInvocation(ContainerExtensionContext extensionContext) {
			if (extensionContext.getTestMethod().isPresent()) {
				Map<Parameter, String[]> values = new LinkedHashMap<>();
				Parameter[] parameters = extensionContext.getTestMethod().get().getParameters();
				for (Parameter parameter : parameters) {
					if (parameter.isAnnotationPresent(StringValues.class)) {
						StringValues annotation = parameter.getAnnotation(StringValues.class);
						String[] array = annotation.value();
						values.put(parameter, array);
					}
				}
				if (values.isEmpty()) {
					return emptyIterator();
				}
				int length = values.values().stream().map(a -> a.length).max(naturalOrder()).orElse(0);
				values.entrySet().stream().filter(entry -> entry.getValue().length != length).findFirst().ifPresent(
					entry -> {
						throw new JUnitException(
							"Illegal entry for parameter " + entry.getKey() + ": " + Arrays.toString(entry.getValue()));
					});
				return new Iterator<TestInvocationContext>() {
					int current = 0;

					@Override
					public boolean hasNext() {
						return current < length;
					}

					@Override
					public TestInvocationContext next() {
						int index = current++;
						return new TestInvocationContext() {
							@Override
							public String getDisplayName() {
								return values.entrySet().stream().map(
									entry -> entry.getKey().getName() + "=" + entry.getValue()[index]).collect(
										joining(", "));
							}

							@Override
							public boolean hasValue(Parameter parameter) {
								return values.containsKey(parameter);
							}

							@Override
							public Object getValue(Parameter parameter) {
								return values.get(parameter)[index];
							}
						};
					}
				};
			}
			return emptyIterator();
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface StringValues {
		String[] value() default {};
	}

}
