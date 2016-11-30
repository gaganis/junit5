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

import java.util.Optional;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * {@link TestDescriptor} for a dynamic test invocation.
 *
 * @since 5.0
 */
class TestInvocationTestDescriptor extends AbstractTestDescriptor {

	static final String TEST_INVOCATION_SEGMENT_TYPE = "test-invocation";

	TestInvocationTestDescriptor(UniqueId uniqueId, String displayName, Optional<TestSource> source) {
		super(uniqueId, displayName);
		source.ifPresent(this::setSource);
	}

	@Override
	public boolean isTest() {
		return true;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

}
