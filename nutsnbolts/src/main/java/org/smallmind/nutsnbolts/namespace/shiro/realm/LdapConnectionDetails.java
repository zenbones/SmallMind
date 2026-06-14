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
package org.smallmind.nutsnbolts.namespace.shiro.realm;

/**
 * Mutable holder for the coordinates and service-account credentials an {@link LdapAuthorizingRealm} uses to reach a
 * directory server. The host, port, and root namespace are assembled into an {@code ldap://host:port/rootNamespace}
 * provider URL, and the user name and password are used to bind for the directory search that locates a candidate user
 * entry. This is a plain JavaBean intended to be populated by setter injection during assembly; it performs no validation
 * and holds the credentials in plain text for the lifetime of the realm.
 */
public class LdapConnectionDetails {

  private String host;
  private String rootNamespace;
  private String userName;
  private String password;
  private int port;

  /**
   * Returns the distinguished name used to bind for directory searches.
   *
   * @return the bind (service-account) user name
   */
  public String getUserName () {

    return userName;
  }

  /**
   * Sets the distinguished name used to bind for directory searches.
   *
   * @param userName the bind (service-account) user name
   */
  public void setUserName (String userName) {

    this.userName = userName;
  }

  /**
   * Returns the password paired with the bind user name.
   *
   * @return the bind (service-account) password
   */
  public String getPassword () {

    return password;
  }

  /**
   * Sets the password paired with the bind user name.
   *
   * @param password the bind (service-account) password
   */
  public void setPassword (String password) {

    this.password = password;
  }

  /**
   * Returns the directory server host name.
   *
   * @return the LDAP server host
   */
  public String getHost () {

    return host;
  }

  /**
   * Sets the directory server host name.
   *
   * @param host the LDAP server host
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Returns the directory server port.
   *
   * @return the LDAP server port
   */
  public int getPort () {

    return port;
  }

  /**
   * Sets the directory server port.
   *
   * @param port the LDAP server port
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Returns the root namespace appended to the provider URL.
   *
   * @return the directory root namespace (base distinguished name)
   */
  public String getRootNamespace () {

    return rootNamespace;
  }

  /**
   * Sets the root namespace appended to the provider URL.
   *
   * @param rootNamespace the directory root namespace (base distinguished name)
   */
  public void setRootNamespace (String rootNamespace) {

    this.rootNamespace = rootNamespace;
  }
}
