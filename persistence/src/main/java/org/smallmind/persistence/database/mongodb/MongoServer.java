package org.smallmind.persistence.database.mongodb;

public class MongoServer {

  private String host;
  private int port = 27017;

  public MongoServer () {

  }

  public MongoServer (String host, int port) {

    this.host = host;
    this.port = port;
  }

  public String getHost () {

    return host;
  }

  public void setHost (String host) {

    this.host = host;
  }

  public int getPort () {

    return port;
  }

  public void setPort (int port) {

    this.port = port;
  }
}