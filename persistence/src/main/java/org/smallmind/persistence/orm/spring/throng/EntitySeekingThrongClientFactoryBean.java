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
package org.smallmind.persistence.orm.spring.throng;

import java.lang.reflect.InvocationTargetException;
import com.mongodb.client.MongoClient;
import org.smallmind.mongodb.throng.ThrongClient;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.ThrongOptions;
import org.smallmind.persistence.orm.throng.ThrongClientFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class EntitySeekingThrongClientFactoryBean implements FactoryBean<ThrongClientFactory>, InitializingBean {

  private ThrongClientFactory throngClientFactory;
  private AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor;
  private MongoClient mongoClient;
  private ThrongOptions throngOptions;
  private String sessionSourceKey;
  private String databaseName;

  public void setAnnotationSeekingBeanFactoryPostProcessor (AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor) {

    this.annotationSeekingBeanFactoryPostProcessor = annotationSeekingBeanFactoryPostProcessor;
  }

  public void setMongoClient (MongoClient mongoClient) {

    this.mongoClient = mongoClient;
  }

  public void setThrongOptions (ThrongOptions throngOptions) {

    this.throngOptions = throngOptions;
  }

  public void setDatabaseName (String databaseName) {

    this.databaseName = databaseName;
  }

  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return ThrongClientFactory.class;
  }

  @Override
  public ThrongClientFactory getObject ()
    throws Exception {

    return throngClientFactory;
  }

  @Override
  public void afterPropertiesSet ()
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    ThrongClient throngClient = new ThrongClient(mongoClient, databaseName, throngOptions, annotationSeekingBeanFactoryPostProcessor.getAnnotatedClasses(sessionSourceKey));

    throngClientFactory = new ThrongClientFactory(throngClient);
  }
}
