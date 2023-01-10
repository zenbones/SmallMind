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

import java.util.Arrays;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MongoClientSettingsFactoryBean implements InitializingBean, FactoryBean<MongoClientSettings> {

  private final MongoClientSettings.Builder settingsBuilder;
  private MongoCredential mongoCredential;
  private ServerAddress[] serverAddresses;
  private CodecRegistry[] codecRegistries;
  private ReadPreference readPreference;
  private ReadConcern readConcern;
  private WriteConcern writeConcern;
  private Boolean retryReads;
  private Boolean retryWrites;
  private Boolean sslEnabled;
  private Integer socketConnectTimeoutMilliseconds;
  private Integer connectionPoolMinSize;
  private Integer connectionPoolMaxSize;
  private Integer connectionPoolMaxConnecting;
  private Integer connectionPoolMaxWaitTimeMilliseconds;
  private Integer connectionPoolMaxConnectionLifeTimeSeconds;
  private Integer connectionPoolMaxConnectionIdleTimeSeconds;

  public MongoClientSettingsFactoryBean () {

    CodecRegistry codecRegistry;

    codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry());
    settingsBuilder = MongoClientSettings.builder().codecRegistry(codecRegistry);
  }

  public void setMongoCredential (MongoCredential mongoCredential) {

    this.mongoCredential = mongoCredential;
  }

  public void setServerAddresses (ServerAddress[] serverAddresses) {

    this.serverAddresses = serverAddresses;
  }

  public void setCodecRegistries (CodecRegistry[] codecRegistries) {

    this.codecRegistries = codecRegistries;
  }

  public void setReadPreference (ReadPreference readPreference) {

    this.readPreference = readPreference;
  }

  public void setReadConcern (ReadConcern readConcern) {

    this.readConcern = readConcern;
  }

  public void setWriteConcern (WriteConcern writeConcern) {

    this.writeConcern = writeConcern;
  }

  public void setRetryReads (Boolean retryReads) {

    this.retryReads = retryReads;
  }

  public void setRetryWrites (Boolean retryWrites) {

    this.retryWrites = retryWrites;
  }

  public void setSslEnabled (Boolean sslEnabled) {

    this.sslEnabled = sslEnabled;
  }

  public void setSocketConnectTimeoutMilliseconds (Integer socketConnectTimeoutMilliseconds) {

    this.socketConnectTimeoutMilliseconds = socketConnectTimeoutMilliseconds;
  }

  public void setConnectionPoolMinSize (Integer connectionPoolMinSize) {

    this.connectionPoolMinSize = connectionPoolMinSize;
  }

  public void setConnectionPoolMaxSize (Integer connectionPoolMaxSize) {

    this.connectionPoolMaxSize = connectionPoolMaxSize;
  }

  public void setConnectionPoolMaxConnecting (Integer connectionPoolMaxConnecting) {

    this.connectionPoolMaxConnecting = connectionPoolMaxConnecting;
  }

  public void setConnectionPoolMaxWaitTimeMilliseconds (Integer connectionPoolMaxWaitTimeMilliseconds) {

    this.connectionPoolMaxWaitTimeMilliseconds = connectionPoolMaxWaitTimeMilliseconds;
  }

  public void setConnectionPoolMaxConnectionLifeTimeSeconds (Integer connectionPoolMaxConnectionLifeTimeSeconds) {

    this.connectionPoolMaxConnectionLifeTimeSeconds = connectionPoolMaxConnectionLifeTimeSeconds;
  }

  public void setConnectionPoolMaxConnectionIdleTimeSeconds (Integer connectionPoolMaxConnectionIdleTimeSeconds) {

    this.connectionPoolMaxConnectionIdleTimeSeconds = connectionPoolMaxConnectionIdleTimeSeconds;
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
  public MongoClientSettings getObject () {

    return settingsBuilder.build();
  }

  @Override
  public void afterPropertiesSet () {

    if (socketConnectTimeoutMilliseconds != null) {
      settingsBuilder.applyToSocketSettings(builder -> builder.connectTimeout(socketConnectTimeoutMilliseconds, MILLISECONDS));
    }
    settingsBuilder.applyToSslSettings(builder -> builder.enabled(Boolean.TRUE.equals(sslEnabled)));
    settingsBuilder.applyToConnectionPoolSettings(builder -> {
      if (connectionPoolMaxSize != null) {
        builder.maxSize(connectionPoolMaxSize);
      }
      if (connectionPoolMinSize != null) {
        builder.minSize(connectionPoolMinSize);
      }
      if (connectionPoolMaxConnecting != null) {
        builder.maxConnecting(connectionPoolMaxConnecting);
      }
      if (connectionPoolMaxWaitTimeMilliseconds != null) {
        builder.maxWaitTime(connectionPoolMaxWaitTimeMilliseconds, MILLISECONDS);
      }
      if (connectionPoolMaxConnectionLifeTimeSeconds != null) {
        builder.maxConnectionLifeTime(connectionPoolMaxConnectionLifeTimeSeconds, SECONDS);
      }
      if (connectionPoolMaxConnectionIdleTimeSeconds != null) {
        builder.maxConnectionIdleTime(connectionPoolMaxConnectionIdleTimeSeconds, SECONDS);
      }
    });
    settingsBuilder.applyToClusterSettings(builder -> builder.hosts(Arrays.asList(serverAddresses)));
    if (readPreference != null) {
      settingsBuilder.readPreference(readPreference);
    }
    if (readConcern != null) {
      settingsBuilder.readConcern(readConcern);
    }
    if (retryReads != null) {
      settingsBuilder.retryReads(retryReads);
    }
    if (writeConcern != null) {
      settingsBuilder.writeConcern(writeConcern);
    }
    if (retryWrites != null) {
      settingsBuilder.retryWrites(retryWrites);
    }
    if (mongoCredential != null) {
      settingsBuilder.credential(mongoCredential);
    }
    settingsBuilder.uuidRepresentation(UuidRepresentation.STANDARD);

    if ((codecRegistries != null) && (codecRegistries.length > 0)) {
      settingsBuilder.codecRegistry(CodecRegistries.fromRegistries(CodecRegistries.fromRegistries(codecRegistries), MongoClient.getDefaultCodecRegistry()));
    }
  }
}
