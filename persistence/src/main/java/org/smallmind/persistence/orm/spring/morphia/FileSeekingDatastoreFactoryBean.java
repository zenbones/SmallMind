/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.orm.spring.morphia;

import java.util.Set;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.smallmind.persistence.orm.ORMInitializationException;
import org.smallmind.persistence.orm.morphia.DatastoreFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class FileSeekingDatastoreFactoryBean implements FactoryBean<DatastoreFactory>, InitializingBean {

  private DatastoreFactory datastoreFactory;
  private MongoClient mongoClient;
  private String sessionSourceKey;
  private String databaseName;
  private boolean useBulkWriteOperations = false;
  private boolean enableShards = false;

  public void setMongoClient (MongoClient mongoClient) {

    this.mongoClient = mongoClient;
  }

  public void setDatabaseName (String databaseName) {

    this.databaseName = databaseName;
  }

  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  public void setUseBulkWriteOperations (boolean useBulkWriteOperations) {

    this.useBulkWriteOperations = useBulkWriteOperations;
  }

  public void setEnableShards (boolean enableShards) {

    this.enableShards = enableShards;
  }

  public Class getObjectType () {

    return DatastoreFactory.class;
  }

  public boolean isSingleton () {

    return true;
  }

  public DatastoreFactory getObject () {

    return datastoreFactory;
  }

  public void afterPropertiesSet () {

    Morphia morphia;
    Datastore datastore;
    Set<Class> entitySet;

    morphia = new Morphia(entitySet = FileSeekingBeanFactoryPostProcessor.getEntitySet(sessionSourceKey));
    morphia.setUseBulkWriteOperations(useBulkWriteOperations);
    datastore = morphia.createDatastore(mongoClient, databaseName);

    if (enableShards) {
      mongoClient.getDatabase("admin").runCommand(new BasicDBObject("enableSharding", databaseName));

      for (Class<?> entityClass : entitySet) {

        Entity entityAnnotation;

        if ((entityAnnotation = entityClass.getAnnotation(Entity.class)) == null) {
          throw new ORMInitializationException("The morphia entity(%s) is missing an @%s annotation", entityClass.getName(), Entity.class.getSimpleName());
        } else {
          mongoClient.getDatabase("admin").runCommand(new BasicDBObject("shardCollection", databaseName + '.' + entityAnnotation.value()).append("key", new BasicDBObject("_id", "hashed")));
        }
      }
    }

    datastore.ensureIndexes();

    datastoreFactory = new DatastoreFactory(datastore);
  }
}
