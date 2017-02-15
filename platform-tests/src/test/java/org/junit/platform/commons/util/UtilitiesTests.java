/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.commons.util.ReflectionUtils.MethodSortOrder.HierarchyDown;
import static org.junit.platform.commons.util.ReflectionUtils.MethodSortOrder.HierarchyUp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Utilities}.
 *
 * @since 1.0
 */
class UtilitiesTests {

	@Test
	void delegationsYieldEqualResults() throws Throwable {
		Utilities util = Utilities.SINGLETON;

		assertEquals(AnnotationUtils.isAnnotated(Probe.class, Tag.class), util.isAnnotated(Probe.class, Tag.class));
		assertEquals(AnnotationUtils.isAnnotated(Probe.class, Override.class),
			util.isAnnotated(Probe.class, Override.class));

		assertEquals(AnnotationUtils.findAnnotation(Probe.class, Tag.class),
			util.findAnnotation(Probe.class, Tag.class));
		assertEquals(AnnotationUtils.findAnnotation(Probe.class, Override.class),
			util.findAnnotation(Probe.class, Override.class));

		Method bMethod = Probe.class.getDeclaredMethod("bMethod");
		assertEquals(AnnotationUtils.findRepeatableAnnotations(bMethod, Tag.class),
			util.findRepeatableAnnotations(bMethod, Tag.class));
		Object expected = assertThrows(PreconditionViolationException.class,
			() -> AnnotationUtils.findRepeatableAnnotations(bMethod, Override.class));
		Object actual = assertThrows(PreconditionViolationException.class,
			() -> util.findRepeatableAnnotations(bMethod, Override.class));
		assertSame(expected.getClass(), actual.getClass(), "expected same exception class");
		assertEquals(expected.toString(), actual.toString(), "expected equal exception toString representation");

		assertEquals(AnnotationUtils.findAnnotatedMethods(Probe.class, Tag.class, HierarchyDown),
			util.findAnnotatedMethods(Probe.class, Tag.class, HierarchyDown));
		assertEquals(AnnotationUtils.findAnnotatedMethods(Probe.class, Tag.class, HierarchyUp),
			util.findAnnotatedMethods(Probe.class, Tag.class, HierarchyUp));

		assertEquals(AnnotationUtils.findAnnotatedMethods(Probe.class, Override.class, HierarchyDown),
			util.findAnnotatedMethods(Probe.class, Override.class, HierarchyDown));
		assertEquals(AnnotationUtils.findAnnotatedMethods(Probe.class, Override.class, HierarchyUp),
			util.findAnnotatedMethods(Probe.class, Override.class, HierarchyUp));

		assertEquals(AnnotationUtils.findPublicAnnotatedFields(Probe.class, String.class, FieldMarker.class),
			util.findPublicAnnotatedFields(Probe.class, String.class, FieldMarker.class));
		assertEquals(AnnotationUtils.findPublicAnnotatedFields(Probe.class, Throwable.class, Override.class),
			util.findPublicAnnotatedFields(Probe.class, Throwable.class, Override.class));
	}

	@Target({ ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface FieldMarker {
	}

	@Tag("class-tag")
	static class Probe {

		@FieldMarker
		public static String publicStaticAnnotatedField = "static";

		@FieldMarker
		public String publicNormalAnnotatedField = "normal";

		@Tag("method-tag")
		void aMethod() {
		}

		@Tag("method-tag-1")
		@Tag("method-tag-2")
		void bMethod() {
		}
	}
}
