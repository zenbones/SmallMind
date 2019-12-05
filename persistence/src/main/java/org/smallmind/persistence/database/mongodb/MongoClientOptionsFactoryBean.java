/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.WriteConcern;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.FactoryBean;

public class MongoClientOptionsFactoryBean implements FactoryBean<MongoClientOptions> {

  private final MongoClientOptions.Builder optionsBuilder;

  public MongoClientOptionsFactoryBean () {

    CodecRegistry codecRegistry;

    codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)), MongoClient.getDefaultCodecRegistry());
    optionsBuilder = MongoClientOptions.builder().codecRegistry(codecRegistry);
  }

  public void setConnectionsPerHost (int connectionsPerHost) {

    optionsBuilder.connectionsPerHost(connectionsPerHost);
  }

  public void setConnectTimeout (int connectTimeout) {

    optionsBuilder.connectTimeout(connectTimeout);
  }

  public void setMaxWaitTime (int maxWaitTime) {

    optionsBuilder.maxWaitTime(maxWaitTime);
  }

  public void setSocketKeepAlive (boolean socketKeepAlive) {

    optionsBuilder.socketKeepAlive(socketKeepAlive);
  }

  public void setSocketTimeout (int socketTimeout) {

    optionsBuilder.socketTimeout(socketTimeout);
  }

  public void setSslEnabled (boolean sslEnabled) {

    optionsBuilder.sslEnabled(sslEnabled);
  }

  public void setThreadsAllowedToBlockForConnectionMultiplier (int threadsAllowedToBlockForConnectionMultiplier) {

    optionsBuilder.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
  }

  public void setWriteConcern (WriteConcern writeConcern) {

    optionsBuilder.writeConcern(writeConcern);
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return MongoClientOptions.class;
  }

  @Override
  public MongoClientOptions getObject () {

    return optionsBuilder.build();
  }
}
