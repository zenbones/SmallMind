package org.smallmind.phalanx.wire.jms.jndi;

import javax.naming.Context;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class JmsConnectionDetails {

  private ComponentPool<Context> contextPool;
  private String destinationName;
  private String connectionFactoryName;
  private String userName;
  private String password;

  public JmsConnectionDetails (ComponentPool<Context> contextPool, String destinationName, String connectionFactoryName, String userName, String password) {

    this.contextPool = contextPool;
    this.destinationName = destinationName;
    this.connectionFactoryName = connectionFactoryName;
    this.userName = userName;
    this.password = password;
  }

  public ComponentPool<Context> getContextPool () {

    return contextPool;
  }

  public String getDestinationName () {

    return destinationName;
  }

  public String getConnectionFactoryName () {

    return connectionFactoryName;
  }

  public String getUserName () {

    return userName;
  }

  public String getPassword () {

    return password;
  }
}