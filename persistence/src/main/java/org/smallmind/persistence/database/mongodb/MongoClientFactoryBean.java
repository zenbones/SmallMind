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
package org.smallmind.persistence.database.mongodb;

import java.util.LinkedList;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MongoClientFactoryBean implements FactoryBean<MongoClient>, InitializingBean, DisposableBean {

  private MongoClient mongoClient;
  private MongoServer[] servers;
  private MongoDatabase[] databases;
  private MongoClientOptions clientOptions;

  public void setServers (MongoServer[] servers) {

    this.servers = servers;
  }

  public void setDatabases (MongoDatabase[] databases) {

    this.databases = databases;
  }

  public void setClientOptions (MongoClientOptions clientOptions) {

    this.clientOptions = clientOptions;
  }

  @Override
  public void afterPropertiesSet () {

    LinkedList<ServerAddress> serverAddresses = new LinkedList<>();
    LinkedList<MongoCredential> credentialList = new LinkedList<>();

    for (MongoDatabase mongoDatabase : databases) {
      if ((mongoDatabase.getUser() != null) && (!mongoDatabase.getUser().isEmpty())) {
//        credentialList.add(MongoCredential.createScramSha1Credential(mongoDatabase.getUser(), "admin", mongoDatabase.getPassword().toCharArray()));
        credentialList.add(MongoCredential.createCredential(mongoDatabase.getUser(), mongoDatabase.getDatabase(), mongoDatabase.getPassword().toCharArray()));
      }
    }
    for (MongoServer mongoServer : servers) {
      serverAddresses.add(new ServerAddress(mongoServer.getHost(), mongoServer.getPort()));
    }

    mongoClient = (serverAddresses.size() == 1) ? new MongoClient(serverAddresses.getFirst(), credentialList, clientOptions) : new MongoClient(serverAddresses, credentialList, clientOptions);
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return MongoClient.class;
  }

  @Override
  public MongoClient getObject () {

    return mongoClient;
  }

  @Override
  public void destroy () {

    mongoClient.close();
  }
}
