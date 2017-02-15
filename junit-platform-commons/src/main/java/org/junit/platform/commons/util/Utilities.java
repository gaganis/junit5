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

import static org.junit.platform.commons.meta.API.Usage.Experimental;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.ReflectionUtils.MethodSortOrder;

/**
 * Common reflection, annotation and scanning utilities.
 *
 * <p>The purpose of this interface is to provide {@code TestEngine} and {@code Extension} authors convenient access to
 * a subset of internal utility methods helping with their implementation. This prevents re-inventing the wheel and
 * ensures that common tasks are handled in the same way.
 */
@API(Experimental)
public interface Utilities {

	/**
	 * Singleton {@link Utilities} instance.
	 */
	Utilities SINGLETON = new Utilities() {
		// empty on purpose
	};

	/**
	 * @see AnnotationUtils#isAnnotated(AnnotatedElement, Class)
	 */
	default boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return AnnotationUtils.isAnnotated(element, annotationType);
	}

	/**
	 * @see AnnotationUtils#findAnnotation(AnnotatedElement, Class)
	 */
	default <A extends Annotation> Optional<A> findAnnotation(AnnotatedElement element, Class<A> annotationType) {
		return AnnotationUtils.findAnnotation(element, annotationType);
	}

	/**
	 * @see AnnotationUtils#findRepeatableAnnotations(AnnotatedElement, Class)
	 */
	default <A extends Annotation> List<A> findRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {
		return AnnotationUtils.findRepeatableAnnotations(element, annotationType);
	}

	/**
	 * @see AnnotationUtils#findPublicAnnotatedFields(Class, Class, Class)
	 */
	default List<Field> findPublicAnnotatedFields(Class<?> clazz, Class<?> fieldType,
			Class<? extends Annotation> annotationType) {
		return AnnotationUtils.findPublicAnnotatedFields(clazz, fieldType, annotationType);
	}

	/**
	 * @see AnnotationUtils#findAnnotatedMethods(Class, Class, ReflectionUtils.MethodSortOrder)
	 */
	default List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType,
			MethodSortOrder sortOrder) {
		return AnnotationUtils.findAnnotatedMethods(clazz, annotationType, sortOrder);
	}
}
