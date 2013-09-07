package org.smallmind.nutsnbolts.security;

public class LdapConnectionDetails {

  private String host;
  private String rootNamespace;
  private String userName;
  private String password;
  private int port;

  public String getUserName () {

    return userName;
  }

  public void setUserName (String userName) {

    this.userName = userName;
  }

  public String getPassword () {

    return password;
  }

  public void setPassword (String password) {

    this.password = password;
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

  public String getRootNamespace () {

    return rootNamespace;
  }

  public void setRootNamespace (String rootNamespace) {

    this.rootNamespace = rootNamespace;
  }
}