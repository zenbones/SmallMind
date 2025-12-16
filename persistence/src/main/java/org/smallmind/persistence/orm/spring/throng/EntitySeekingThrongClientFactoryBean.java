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
package org.smallmind.persistence.orm.spring.throng;

import java.lang.reflect.InvocationTargetException;
import com.mongodb.client.MongoClient;
import org.smallmind.mongodb.throng.ThrongClient;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.ThrongOptions;
import org.smallmind.persistence.orm.throng.ThrongClientFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs a {@link ThrongClientFactory}. It gathers MongoDB
 * entity classes discovered by {@link AnnotationSeekingBeanFactoryPostProcessor}, creates a
 * {@link ThrongClient} with the supplied {@link MongoClient} and options, and exposes it as a
 * singleton factory.
 */
public class EntitySeekingThrongClientFactoryBean implements FactoryBean<ThrongClientFactory>, InitializingBean {

  private ThrongClientFactory throngClientFactory;
  private AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor;
  private MongoClient mongoClient;
  private ThrongOptions throngOptions;
  private String sessionSourceKey;
  private String databaseName;

  /**
   * Sets the helper responsible for collecting Throng-mapped classes.
   *
   * @param annotationSeekingBeanFactoryPostProcessor post-processor used to look up entity classes
   */
  public void setAnnotationSeekingBeanFactoryPostProcessor (AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor) {

    this.annotationSeekingBeanFactoryPostProcessor = annotationSeekingBeanFactoryPostProcessor;
  }

  /**
   * Supplies the Mongo client to back the Throng client.
   *
   * @param mongoClient configured Mongo client
   */
  public void setMongoClient (MongoClient mongoClient) {

    this.mongoClient = mongoClient;
  }

  /**
   * Sets optional Throng configuration.
   *
   * @param throngOptions Throng options to apply when creating the client
   */
  public void setThrongOptions (ThrongOptions throngOptions) {

    this.throngOptions = throngOptions;
  }

  /**
   * Sets the MongoDB database name to use.
   *
   * @param databaseName database name
   */
  public void setDatabaseName (String databaseName) {

    this.databaseName = databaseName;
  }

  /**
   * Narrows the entity scan to a specific session source.
   *
   * @param sessionSourceKey session source key or {@code null} for default
   */
  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<?> getObjectType () {

    return ThrongClientFactory.class;
  }

  /**
   * {@inheritDoc}
   *
   * @return singleton {@link ThrongClientFactory}
   * @throws Exception if the factory has not been initialized
   */
  @Override
  public ThrongClientFactory getObject ()
    throws Exception {

    return throngClientFactory;
  }

  /**
   * Instantiates the {@link ThrongClient} using discovered entity classes and wraps it in a
   * {@link ThrongClientFactory}.
   *
   * @throws ThrongMappingException    if entity mapping fails
   * @throws NoSuchMethodException     if an expected constructor is missing
   * @throws InstantiationException    if the client cannot be instantiated
   * @throws IllegalAccessException    if the constructor is not accessible
   * @throws InvocationTargetException if the constructor throws an exception
   */
  @Override
  public void afterPropertiesSet ()
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    ThrongClient throngClient = new ThrongClient(mongoClient, databaseName, throngOptions, annotationSeekingBeanFactoryPostProcessor.getAnnotatedClasses(sessionSourceKey));

    throngClientFactory = new ThrongClientFactory(throngClient);
  }
}
