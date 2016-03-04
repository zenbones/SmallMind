package org.smallmind.persistence.database.mongodb;

import java.util.LinkedList;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.springframework.beans.factory.FactoryBean;

public class MongoClientFactoryBean implements FactoryBean<MongoClient> {

  private MongoServer[] servers;
  private MongoDatabase[] databases;
  private MongoClientOptions clientOptions;
  private WriteConcern writeConcern = WriteConcern.JOURNALED;

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
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return MongoClient.class;
  }

  @Override
  public MongoClient getObject () {

    MongoClient mongoClient;

    LinkedList<ServerAddress> serverAddresses = new LinkedList<>();
    LinkedList<MongoCredential> credentialList = new LinkedList<>();

    for (MongoDatabase mongoDatabase : databases) {
      if ((mongoDatabase.getUser() != null) && (!mongoDatabase.getUser().isEmpty())) {
        credentialList.add(MongoCredential.createCredential(mongoDatabase.getUser(), mongoDatabase.getDatabase(), mongoDatabase.getPassword().toCharArray()));
      }
    }
    for (MongoServer mongoServer : servers) {
      serverAddresses.add(new ServerAddress(mongoServer.getHost(), mongoServer.getPort()));
    }

    mongoClient = (serverAddresses.size() == 1) ? new MongoClient(serverAddresses.getFirst(), credentialList, clientOptions) : new MongoClient(serverAddresses, credentialList, clientOptions);
    mongoClient.setWriteConcern(writeConcern);

    return mongoClient;
  }
}
