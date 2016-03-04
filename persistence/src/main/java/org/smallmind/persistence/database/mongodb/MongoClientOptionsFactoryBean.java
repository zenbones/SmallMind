package org.smallmind.persistence.database.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.FactoryBean;

public class MongoClientOptionsFactoryBean implements FactoryBean<MongoClientOptions> {

  private final MongoClientOptions.Builder optionsBuilder;

  private boolean socketKeepAlive;
  private int connectionsPerHost;
  private int threadsAllowedToBlockForConnectionMultiplier;
  private int connectTimeout;
  private int maxWaitTime;
  private int socketTimeout;

  public MongoClientOptionsFactoryBean() {

    CodecRegistry codecRegistry;

    codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)), MongoClient.getDefaultCodecRegistry());
    optionsBuilder = MongoClientOptions.builder().codecRegistry(codecRegistry);
  }

  public void setConnectionsPerHost(int connectionsPerHost) {

    optionsBuilder.connectionsPerHost(connectionsPerHost);
  }

  public void setConnectTimeout(int connectTimeout) {

    optionsBuilder.connectTimeout(connectTimeout);
  }

  public void setMaxWaitTime(int maxWaitTime) {

    optionsBuilder.maxWaitTime(maxWaitTime);
  }

  public void setSocketKeepAlive(boolean socketKeepAlive) {

    optionsBuilder.socketKeepAlive(socketKeepAlive);
  }

  public void setSocketTimeout(int socketTimeout) {

    optionsBuilder.socketTimeout(socketTimeout);
  }

  public void setThreadsAllowedToBlockForConnectionMultiplier(int threadsAllowedToBlockForConnectionMultiplier) {

    optionsBuilder.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
  }

  @Override
  public boolean isSingleton() {

    return true;
  }

  @Override
  public Class<?> getObjectType() {

    return MongoClientOptions.class;
  }

  @Override
  public MongoClientOptions getObject() throws Exception {

    return optionsBuilder.build();
  }
}
