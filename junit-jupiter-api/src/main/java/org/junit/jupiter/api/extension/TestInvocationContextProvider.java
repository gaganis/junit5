/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.api.extension;

import static java.util.Collections.emptyList;
import static org.junit.platform.commons.meta.API.Usage.Experimental;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.junit.platform.commons.meta.API;

/**
 * {@code TestInvocationContextProvider} defines the API for
 * {@link Extension Extensions} that wish to provide one or multiple contexts
 * for the invocation of a test method.
 *
 * <p>This extension point makes it possible to execute a test in different
 * contexts, e.g. with different parameters or by preparing the test class
 * instance differently, or multiple times without modifying the context.
 *
 * <p>This interface defines two methods: {@link #supports} and
 * {@link #provide}. The former is called by the framework to determine whether
 * this extension wants to act on a test that is about to be executed. If so,
 * the latter is called and must return an {@link Iterator} of
 * {@link TestInvocationContext} instances. Otherwise, this provider is ignored
 * for the execution of the current test.
 *
 * <p>A provider that has returned {@code true} from its {@link #supports}
 * method it is called <em>active</em>. When multiple providers are active for
 * a test method, the {@code Iterators} returned by their {@link #provide}
 * methods will be chained, i.e. the test method will be invoked using the
 * contexts of all active providers.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * @see TestInvocationContext
 * @since 5.0
 */
@API(Experimental)
public interface TestInvocationContextProvider extends Extension {

	/**
	 * Determine if this provider supports providing test invocation contexts
	 * for the test method represented by the supplied {@code context}.
	 *
	 * @param context the container extension context for the test method about
	 * to be invoked; never {@code null}
	 * @return {@code true} if this provider can provide test invocation contexts
	 * @see #provide
	 * @see ContainerExtensionContext
	 */
	boolean supports(ContainerExtensionContext context);

	/**
	 * Provide {@link TestInvocationContext TestInvocationContexts} for the test
	 * method represented by the supplied {@code context}.
	 *
	 * <p>This method is only called by the framework if {@link #supports} has
	 * previously returned {@code true} for the same
	 * {@link ContainerExtensionContext}. It must not return an empty
	 * {@code Iterator}.
	 *
	 * @param context the container extension context for the test method about
	 * to be invoked; never {@code null}
	 * @return an Iterator of TestInvocationContext instances for the invocation
	 * of the test method; never {@code null} or empty
	 * @see #supports
	 * @see ContainerExtensionContext
	 */
	Iterator<TestInvocationContext> provide(ContainerExtensionContext context);

	/**
	 * {@code TestInvocationContextProvider} represents the context of a
	 * dynamic test method invocation.
	 *
	 * <p>A context consists of a custom display name to represent the
	 * invocation in the test tree and a list of {@link Extension Extensions}
	 * that will be added to the already registered extensions for the test
	 * method but only for this invocation.
	 *
	 * <p>Since the extensions provided by this context will only be used for a
	 * single test invocation, it does not make sense to return an extension
	 * that acts solely on the container level (e.g. {@link BeforeAllCallback}).
	 *
	 * @since 5.0
	 */
	interface TestInvocationContext {

		/**
		 * Get the display name of the invocation of the test method
		 * represented by the supplied {@code context} with the supplied
		 * {@code index}.
		 *
		 * <p>The supplied {@code index} is incremented by the framework with
		 * each test invocation. Thus, in the case of multiple active
		 * providers, only the first active provider will receive indices
		 * starting with {@code 0}.
		 *
		 * @param context the container extension context for the test method
		 * about to be invoked; never {@code null}
		 * @param index the current invocation index (zero based) of the test
		 * method
		 * @return the display name of the test invocation; never {@code null}
		 * or blank
		 * @see ContainerExtensionContext
		 */
		default String getDisplayName(ContainerExtensionContext context, int index) {
			return MessageFormat.format("{0}[{1}]", context.getDisplayName(), index);
		}

		/**
		 * Get the list of additional {@link Extension Extensions} of the test
		 * invocation.
		 *
		 * @return the list of extensions; may be empty but never {@code null}
		 */
		default List<Extension> getExtensions() {
			return emptyList();
		}

	}

}
