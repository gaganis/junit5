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

import static org.junit.platform.commons.meta.API.Usage.Experimental;

import java.util.Iterator;

import org.junit.platform.commons.meta.API;

/**
 * {@code TestInvocationContextProvider} defines the API for
 * {@link Extension Extensions} that wish to provide one or multiple information for the invocation of tests.
 *
 * @since 5.0
 */
@API(Experimental)
public interface TestInvocationContextProvider extends Extension {

	Iterator<TestInvocationContext> provideInvocation(ContainerExtensionContext extensionContext);

}
