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
package org.smallmind.mongodb.utility.spring;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.ssl.TrustManagerUtility;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Spring factory bean for constructing {@link MongoClientSettings} with convenient setters for common options.
 */
public class MongoClientSettingsFactoryBean implements InitializingBean, FactoryBean<MongoClientSettings> {

  private MongoClientSettings.Builder settingsBuilder;
  private CodecRegistry codecRegistry;
  private Resource certResource;
  private MongoCredential mongoCredential;
  private ServerAddress[] serverAddresses;
  private ReadPreference readPreference;
  private ReadConcern readConcern;
  private WriteConcern writeConcern;
  private Boolean retryReads;
  private Boolean retryWrites;
  private Boolean sslEnabled;
  private Boolean allowInvalidHostNames;
  private Boolean writeConcernEnabled;
  private Integer socketConnectTimeoutMilliseconds;
  private Integer serverSelectionTimeoutMilliseconds;
  private Integer connectionPoolMinSize;
  private Integer connectionPoolMaxSize;
  private Integer connectionPoolMaxConnecting;
  private Integer connectionPoolMaxWaitTimeMilliseconds;
  private Integer connectionPoolMaxConnectionLifeTimeSeconds;
  private Integer connectionPoolMaxConnectionIdleTimeSeconds;

  /**
   * Sets an additional codec registry to merge with the driver's defaults.
   *
   * @param codecRegistry the codec registry to prepend when building the settings
   */
  public void setCodecRegistry (CodecRegistry codecRegistry) {

    this.codecRegistry = codecRegistry;
  }

  /**
   * Sets the TLS certificate resource used to build the SSL context.
   *
   * @param certResource the resource containing the trusted certificate
   */
  public void setCertResource (Resource certResource) {

    this.certResource = certResource;
  }

  /**
   * Sets the credentials used to authenticate with the server.
   *
   * @param mongoCredential the authentication credential
   */
  public void setMongoCredential (MongoCredential mongoCredential) {

    this.mongoCredential = mongoCredential;
  }

  /**
   * Sets the list of server addresses used to configure the cluster.
   *
   * @param serverAddresses the seed list of server addresses
   */
  public void setServerAddresses (ServerAddress[] serverAddresses) {

    this.serverAddresses = serverAddresses;
  }

  /**
   * Sets the read preference applied to all read operations.
   *
   * @param readPreference the desired read preference
   */
  public void setReadPreference (ReadPreference readPreference) {

    this.readPreference = readPreference;
  }

  /**
   * Sets the read concern applied to all read operations.
   *
   * @param readConcern the desired read concern
   */
  public void setReadConcern (ReadConcern readConcern) {

    this.readConcern = readConcern;
  }

  /**
   * Sets the write concern applied to all write operations.
   *
   * @param writeConcern the desired write concern
   */
  public void setWriteConcern (WriteConcern writeConcern) {

    this.writeConcern = writeConcern;
  }

  /**
   * Controls whether the configured write concern is honored; set to {@code false} to suppress it.
   *
   * @param writeConcernEnabled {@code false} to ignore the configured write concern
   */
  public void setWriteConcernEnabled (Boolean writeConcernEnabled) {

    this.writeConcernEnabled = writeConcernEnabled;
  }

  /**
   * Sets whether retryable reads are enabled.
   *
   * @param retryReads {@code true} to enable automatic read retries
   */
  public void setRetryReads (Boolean retryReads) {

    this.retryReads = retryReads;
  }

  /**
   * Sets whether retryable writes are enabled.
   *
   * @param retryWrites {@code true} to enable automatic write retries
   */
  public void setRetryWrites (Boolean retryWrites) {

    this.retryWrites = retryWrites;
  }

  /**
   * Sets whether SSL/TLS is enabled for server connections.
   *
   * @param sslEnabled {@code true} to require TLS
   */
  public void setSslEnabled (Boolean sslEnabled) {

    this.sslEnabled = sslEnabled;
  }

  /**
   * Sets whether invalid host names are allowed when SSL is enabled.
   *
   * @param allowInvalidHostNames {@code true} to skip host name verification
   */
  public void setAllowInvalidHostNames (Boolean allowInvalidHostNames) {

    this.allowInvalidHostNames = allowInvalidHostNames;
  }

  /**
   * Sets the socket connection timeout.
   *
   * @param socketConnectTimeoutMilliseconds timeout in milliseconds
   */
  public void setSocketConnectTimeoutMilliseconds (Integer socketConnectTimeoutMilliseconds) {

    this.socketConnectTimeoutMilliseconds = socketConnectTimeoutMilliseconds;
  }

  /**
   * Sets the server selection timeout.
   *
   * @param serverSelectionTimeoutMilliseconds timeout in milliseconds
   */
  public void setServerSelectionTimeoutMilliseconds (Integer serverSelectionTimeoutMilliseconds) {

    this.serverSelectionTimeoutMilliseconds = serverSelectionTimeoutMilliseconds;
  }

  /**
   * Sets the minimum number of connections maintained in the pool.
   *
   * @param connectionPoolMinSize minimum pool size
   */
  public void setConnectionPoolMinSize (Integer connectionPoolMinSize) {

    this.connectionPoolMinSize = connectionPoolMinSize;
  }

  /**
   * Sets the maximum number of connections allowed in the pool.
   *
   * @param connectionPoolMaxSize maximum pool size
   */
  public void setConnectionPoolMaxSize (Integer connectionPoolMaxSize) {

    this.connectionPoolMaxSize = connectionPoolMaxSize;
  }

  /**
   * Sets the maximum number of connections that may be establishing concurrently.
   *
   * @param connectionPoolMaxConnecting maximum number of concurrent connecting threads
   */
  public void setConnectionPoolMaxConnecting (Integer connectionPoolMaxConnecting) {

    this.connectionPoolMaxConnecting = connectionPoolMaxConnecting;
  }

  /**
   * Sets the maximum time a thread will wait to acquire a connection from the pool.
   *
   * @param connectionPoolMaxWaitTimeMilliseconds wait timeout in milliseconds
   */
  public void setConnectionPoolMaxWaitTimeMilliseconds (Integer connectionPoolMaxWaitTimeMilliseconds) {

    this.connectionPoolMaxWaitTimeMilliseconds = connectionPoolMaxWaitTimeMilliseconds;
  }

  /**
   * Sets the maximum total lifetime for pooled connections.
   *
   * @param connectionPoolMaxConnectionLifeTimeSeconds maximum lifetime in seconds
   */
  public void setConnectionPoolMaxConnectionLifeTimeSeconds (Integer connectionPoolMaxConnectionLifeTimeSeconds) {

    this.connectionPoolMaxConnectionLifeTimeSeconds = connectionPoolMaxConnectionLifeTimeSeconds;
  }

  /**
   * Sets the maximum idle time before a pooled connection is eligible for eviction.
   *
   * @param connectionPoolMaxConnectionIdleTimeSeconds maximum idle time in seconds
   */
  public void setConnectionPoolMaxConnectionIdleTimeSeconds (Integer connectionPoolMaxConnectionIdleTimeSeconds) {

    this.connectionPoolMaxConnectionIdleTimeSeconds = connectionPoolMaxConnectionIdleTimeSeconds;
  }

  /**
   * Returns {@code true}; the configured settings object is a shared singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns {@code MongoClientSettings.class}.
   *
   * @return {@code MongoClientSettings.class}
   */
  @Override
  public Class<?> getObjectType () {

    return MongoClientSettings.class;
  }

  /**
   * Returns the built {@link MongoClientSettings}.
   *
   * @return the constructed settings
   */
  @Override
  public MongoClientSettings getObject () {

    return settingsBuilder.build();
  }

  /**
   * Builds the {@link MongoClientSettings} instance from the configured properties.
   */
  @Override
  public void afterPropertiesSet ()
    throws IOException, ResourceException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

    SSLContext sslContext;

    settingsBuilder = MongoClientSettings.builder();

    if (socketConnectTimeoutMilliseconds != null) {
      settingsBuilder.applyToSocketSettings(builder -> builder.connectTimeout(socketConnectTimeoutMilliseconds, MILLISECONDS));
    }

    if (certResource == null) {
      sslContext = null;
    } else {
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, TrustManagerUtility.load("tlsCert", certResource), new SecureRandom());
    }

    settingsBuilder.applyToSslSettings(builder -> {
      if (sslContext != null) {
        builder.context(sslContext);
      }

      builder.enabled(Boolean.TRUE.equals(sslEnabled)).invalidHostNameAllowed(Boolean.TRUE.equals(allowInvalidHostNames));
    });

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
    settingsBuilder.applyToClusterSettings(builder -> {
      builder.hosts(Arrays.asList(serverAddresses));

      if (serverSelectionTimeoutMilliseconds != null) {
        builder.serverSelectionTimeout(serverSelectionTimeoutMilliseconds, MILLISECONDS);
      }
    });
    if (readPreference != null) {
      settingsBuilder.readPreference(readPreference);
    }
    if (readConcern != null) {
      settingsBuilder.readConcern(readConcern);
    }
    if (retryReads != null) {
      settingsBuilder.retryReads(retryReads);
    }
    if ((writeConcern != null) && (!Boolean.FALSE.equals(writeConcernEnabled))) {
      settingsBuilder.writeConcern(writeConcern);
    }
    if (retryWrites != null) {
      settingsBuilder.retryWrites(retryWrites);
    }
    if (mongoCredential != null) {
      settingsBuilder.credential(mongoCredential);
    }
    if (codecRegistry != null) {
      settingsBuilder.codecRegistry(CodecRegistries.fromRegistries(codecRegistry, MongoClientSettings.getDefaultCodecRegistry()));
    }

    settingsBuilder.uuidRepresentation(UuidRepresentation.STANDARD);
  }
}
