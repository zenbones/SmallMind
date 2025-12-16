/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.spring;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import org.smallmind.persistence.ManagedDao;
import org.smallmind.persistence.ManagedDaoSupport;
import org.smallmind.persistence.orm.MappedRelationships;
import org.smallmind.persistence.orm.MappedSubClasses;
import org.smallmind.persistence.orm.SessionSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Base {@link BeanFactoryPostProcessor} that discovers DAO beans in the Spring context, inspects
 * their durable entity types, and records any classes annotated with a configured set of metadata
 * annotations. Subclasses specify which DAO implementations to consider and which annotations mark
 * persistent classes. The collected classes can then be reused later (e.g. to build persistence
 * units or mapping registries).
 */
public abstract class AbstractAnnotationSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  private static final Class[] NO_CLASSES = new Class[0];
  private final HashMap<String, HashSet<Class<?>>> annotatedClassMap = new HashMap<>();

  /**
   * DAO interfaces or base classes that identify beans participating in the scan.
   *
   * @return managed DAO types that should trigger entity discovery
   */
  public abstract Class<? extends ManagedDao<?, ?>>[] getDaoImplementations ();

  /**
   * Annotations that mark classes as persistent entities.
   *
   * @return annotation types to search for
   */
  public abstract Class<? extends Annotation>[] getTargetAnnotations ();

  /**
   * Returns all discovered annotated classes regardless of {@link SessionSource} key.
   *
   * @return array of discovered classes, possibly empty
   */
  public Class[] getAnnotatedClasses () {

    return getAnnotatedClasses(null);
  }

  /**
   * Returns discovered classes associated with a particular session source key.
   *
   * @param sessionSourceKey {@link SessionSource#value()} associated with the DAO, or {@code null}
   * @return array of classes for the key, or empty array if none were found
   */
  public Class[] getAnnotatedClasses (String sessionSourceKey) {

    Class[] annotatedClasses;
    HashSet<Class<?>> annotatedClassSet;

    if ((annotatedClassSet = annotatedClassMap.get(sessionSourceKey)) == null) {
      return NO_CLASSES;
    }

    annotatedClasses = new Class[annotatedClassSet.size()];
    annotatedClassSet.toArray(annotatedClasses);

    return annotatedClasses;
  }

  /**
   * Scans bean definitions for known DAO types, infers their durable entity class, and records any
   * entities annotated with the configured target annotations. Also walks mapped subclasses and
   * relationships to ensure all related entity types are captured.
   *
   * @param configurableListableBeanFactory the bean factory to scan
   * @throws BeansException when DAO metadata cannot be resolved or a mapping is invalid
   */
  public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    Class<?> beanClass;

    for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {
      if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
        if (isDaoImplementation(beanClass)) {

          Class<?> persistentClass;

          if ((persistentClass = ManagedDaoSupport.findDurableClass(beanClass)) == null) {
            throw new FatalBeanException("No inference of the Durable class for type(" + beanClass.getName() + ") was possible");
          } else {

            SessionSource dataSourceAnnotation;
            HashSet<Class<?>> annotatedClassSet;

            String sessionSourceKey = null;

            if ((dataSourceAnnotation = beanClass.getAnnotation(SessionSource.class)) != null) {
              sessionSourceKey = dataSourceAnnotation.value();
            }

            if ((annotatedClassSet = annotatedClassMap.get(sessionSourceKey)) == null) {
              annotatedClassMap.put(sessionSourceKey, annotatedClassSet = new HashSet<>());
            }

            processClass(persistentClass, annotatedClassSet);
          }
        }
      }
    }
  }

  /**
   * Recursively records the persistent class and any mapped subclasses or relationships if they
   * carry target annotations.
   *
   * @param persistentClass   entity class to process
   * @param annotatedClassSet set tracking classes for a given session source
   */
  private void processClass (Class<?> persistentClass, HashSet<Class<?>> annotatedClassSet) {

    if (hasTargetAnnotation(persistentClass)) {

      MappedSubClasses mappedSubClasses;
      MappedRelationships mappedRelationships;

      annotatedClassSet.add(persistentClass);

      if ((mappedSubClasses = persistentClass.getAnnotation(MappedSubClasses.class)) != null) {
        for (Class<?> subclass : mappedSubClasses.value()) {
          if (!persistentClass.isAssignableFrom(subclass)) {
            throw new FatalBeanException("Mapped subclass of type (" + subclass.getName() + ") must inherit from parent type (" + persistentClass.getName() + ")");
          }

          processClass(subclass, annotatedClassSet);
        }
      }

      if ((mappedRelationships = persistentClass.getAnnotation(MappedRelationships.class)) != null) {
        for (Class<?> relatedClass : mappedRelationships.value()) {
          processClass(relatedClass, annotatedClassSet);
        }
      }
    }
  }

  /**
   * Determines whether the bean class is one of the configured DAO implementations.
   *
   * @param beanClass candidate bean class
   * @return {@code true} if the bean should trigger entity discovery
   */
  private boolean isDaoImplementation (Class<?> beanClass) {

    for (Class<? extends ManagedDao<?, ?>> daoImplementation : getDaoImplementations()) {
      if (daoImplementation.isAssignableFrom(beanClass)) {

        return true;
      }
    }

    return false;
  }

  /**
   * Checks whether the persistent class is annotated with any of the target annotations.
   *
   * @param persistentClass class to inspect
   * @return {@code true} when at least one target annotation is present
   */
  private boolean hasTargetAnnotation (Class<?> persistentClass) {

    for (Class<? extends Annotation> targetAnnotation : getTargetAnnotations()) {
      if (persistentClass.isAnnotationPresent(targetAnnotation)) {

        return true;
      }
    }

    return false;
  }
}
