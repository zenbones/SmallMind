/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.persistence.orm.spring.jpa;

import java.lang.annotation.Annotation;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.smallmind.persistence.ManagedDao;
import org.smallmind.persistence.orm.jpa.JPADao;
import org.smallmind.persistence.orm.querydsl.jpa.QJPADao;
import org.smallmind.persistence.orm.spring.AbstractAnnotationSeekingBeanFactoryPostProcessor;

public class AnnotationSeekingBeanFactoryPostProcessor extends AbstractAnnotationSeekingBeanFactoryPostProcessor {

  private static final Class<? extends ManagedDao<?, ?>>[] DAO_IMPLEMENTATIONS = new Class[] {JPADao.class, QJPADao.class};
  private static final Class<? extends Annotation>[] TARGET_ANNOTATIONS = new Class[] {Entity.class, Embeddable.class, MappedSuperclass.class};

  @Override
  public Class<? extends ManagedDao<?, ?>>[] getDaoImplementations () {

    return DAO_IMPLEMENTATIONS;
  }

  @Override
  public Class<? extends Annotation>[] getTargetAnnotations () {

    return TARGET_ANNOTATIONS;
  }
}
