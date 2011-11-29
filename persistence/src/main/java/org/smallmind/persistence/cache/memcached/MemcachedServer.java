package org.smallmind.persistence.cache.memcached;

public class MemcachedServer {

  private String host;
  private int port;

  public MemcachedServer () {

  }

  public MemcachedServer (String host, int port) {

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
