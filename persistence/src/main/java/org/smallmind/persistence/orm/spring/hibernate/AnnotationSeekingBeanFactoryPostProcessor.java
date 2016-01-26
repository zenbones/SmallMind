/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.orm.spring.hibernate;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import org.smallmind.persistence.orm.SessionSource;
import org.smallmind.persistence.orm.hibernate.HibernateDao;
import org.smallmind.persistence.spring.ManagedDaoSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class AnnotationSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  private static final HashMap<String, HashSet<Class>> ANNOTATED_CLASS_DATA_SOURCE_MAP = new HashMap<String, HashSet<Class>>();

  private static final Class[] NO_CLASSES = new Class[0];

  private Class<? extends Annotation>[] markedAnnotations;

  public static Class[] getAnnotatedClasses () {

    return getAnnotatedClasses(null);
  }

  public static Class[] getAnnotatedClasses (String sessionSourceKey) {

    Class[] annotatedClasses;
    HashSet<Class> annotatedClassSet;

    if ((annotatedClassSet = ANNOTATED_CLASS_DATA_SOURCE_MAP.get(sessionSourceKey)) == null) {
      return NO_CLASSES;
    }

    annotatedClasses = new Class[annotatedClassSet.size()];
    annotatedClassSet.toArray(annotatedClasses);

    return annotatedClasses;
  }

  public void setMarkedAnnotations (Class<? extends Annotation>[] markedAnnotations) {

    this.markedAnnotations = markedAnnotations;
  }

  private boolean hasMarkedAnnotation (Class persistentClass) {

    for (Class<? extends Annotation> markedAnnotation : markedAnnotations) {
      if (persistentClass.isAnnotationPresent(markedAnnotation)) {

        return true;
      }
    }

    return false;
  }

  public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
    throws BeansException {

    Class<?> beanClass;
    Class persistentClass;
    Annotation dataSourceAnnotation;
    HashSet<Class> annotatedClassSet;
    String sessionSourceKey = null;

    for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {
      if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
        if (HibernateDao.class.isAssignableFrom(beanClass)) {
          if ((dataSourceAnnotation = beanClass.getAnnotation(SessionSource.class)) != null) {
            sessionSourceKey = ((SessionSource)dataSourceAnnotation).value();
          }

          if ((annotatedClassSet = ANNOTATED_CLASS_DATA_SOURCE_MAP.get(sessionSourceKey)) == null) {
            ANNOTATED_CLASS_DATA_SOURCE_MAP.put(sessionSourceKey, annotatedClassSet = new HashSet<Class>());
          }

          if ((persistentClass = ManagedDaoSupport.findDurableClass(beanClass)) == null) {
            throw new FatalBeanException("No inference of the Durable class for type(" + beanClass.getName() + ") was possible");
          }
          else if (hasMarkedAnnotation(persistentClass)) {
            annotatedClassSet.add(persistentClass);
          }
        }
      }
    }
  }
}