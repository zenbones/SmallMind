/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.messaging;

import org.smallmind.quorum.pool2.ConnectionPool;

public class MessagingConnectionDetails {

  public static final String SELECTION_PROPERTY = "Selection";
  public static final String EXCEPTION_PROPERTY = "Exception";

  private ConnectionPool contextPool;
  private String destinationName;
  private String connectionFactoryName;
  private String userName;
  private String password;
  private String serviceSelector;
  private int transmissionPoolSize;

  public MessagingConnectionDetails (ConnectionPool contextPool, String destinationName, String connectionFactoryName, String userName, String password, int transmissionPoolSize) {

    this(contextPool, destinationName, connectionFactoryName, userName, password, transmissionPoolSize, null);
  }

  public MessagingConnectionDetails (ConnectionPool contextPool, String destinationName, String connectionFactoryName, String userName, String password, int transmissionPoolSize, String serviceSelector) {

    this.contextPool = contextPool;
    this.destinationName = destinationName;
    this.connectionFactoryName = connectionFactoryName;
    this.userName = userName;
    this.password = password;
    this.transmissionPoolSize = transmissionPoolSize;
    this.serviceSelector = serviceSelector;
  }

  public ConnectionPool getContextPool () {

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

  public String getServiceSelector () {

    return serviceSelector;
  }

  public int getTransmissionPoolSize () {

    return transmissionPoolSize;
  }
}
