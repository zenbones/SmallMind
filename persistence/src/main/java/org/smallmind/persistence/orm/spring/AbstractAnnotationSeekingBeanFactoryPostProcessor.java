/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public abstract class AbstractAnnotationSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  private static final Class[] NO_CLASSES = new Class[0];
  private final HashMap<String, HashSet<Class<?>>> annotatedClassMap = new HashMap<>();

  public abstract Class<? extends ManagedDao<?,?>>[] getDaoImplementations ();

  public abstract Class<? extends Annotation>[] getTargetAnnotations ();

  public Class[] getAnnotatedClasses () {

    return getAnnotatedClasses(null);
  }

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

  private boolean isDaoImplementation (Class<?> beanClass) {

    for (Class<? extends ManagedDao<?,?>> daoImplementation : getDaoImplementations()) {
      if (daoImplementation.isAssignableFrom(beanClass)) {

        return true;
      }
    }

    return false;
  }

  private boolean hasTargetAnnotation (Class<?> persistentClass) {

    for (Class<? extends Annotation> targetAnnotation : getTargetAnnotations()) {
      if (persistentClass.isAnnotationPresent(targetAnnotation)) {

        return true;
      }
    }

    return false;
  }
}