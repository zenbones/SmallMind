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
package org.smallmind.persistence.orm.spring.data.mongo;

import com.mongodb.client.MongoClient;
import org.smallmind.persistence.orm.spring.data.mongo.internal.MongoDataConverter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

public class EntitySeekingMongoTemplateFactoryBean implements FactoryBean<MongoTemplateFactory>, InitializingBean {

  private MongoTemplateFactory mongoTemplateFactory;
  private AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor;
  private MongoClient mongoClient;
  private MongoDataEntityCallbacks mongoDataEntityCallbacks;
  private String sessionSourceKey;
  private String databaseName;
  private boolean ensureIndexes = false;

  public void setAnnotationSeekingBeanFactoryPostProcessor (AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor) {

    this.annotationSeekingBeanFactoryPostProcessor = annotationSeekingBeanFactoryPostProcessor;
  }

  public void setMongoClient (MongoClient mongoClient) {

    this.mongoClient = mongoClient;
  }

  public void setMongoDataEntityCallbacks (MongoDataEntityCallbacks mongoDataEntityCallbacks) {

    this.mongoDataEntityCallbacks = mongoDataEntityCallbacks;
  }

  public void setDatabaseName (String databaseName) {

    this.databaseName = databaseName;
  }

  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  public void setEnsureIndexes (boolean ensureIndexes) {

    this.ensureIndexes = ensureIndexes;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return MongoTemplateFactory.class;
  }

  @Override
  public MongoTemplateFactory getObject ()
    throws Exception {

    return mongoTemplateFactory;
  }

  @Override
  public void afterPropertiesSet () {

    MongoDatabaseFactory mongoDatabaseFactory = new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    MongoTemplate mongoTemplate = new MongoTemplate(mongoDatabaseFactory, new MongoDataConverter(mongoDatabaseFactory, ensureIndexes, annotationSeekingBeanFactoryPostProcessor.getAnnotatedClasses(sessionSourceKey)));

    if (mongoDataEntityCallbacks != null) {
      mongoTemplate.setEntityCallbacks(mongoDataEntityCallbacks);
    }

    mongoTemplateFactory = new MongoTemplateFactory(mongoTemplate);
  }
}
