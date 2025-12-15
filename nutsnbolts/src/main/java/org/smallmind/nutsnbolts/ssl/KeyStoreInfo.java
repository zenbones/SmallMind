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
package org.smallmind.nutsnbolts.ssl;

import java.nio.file.Path;

/**
 * Encapsulates the details required to identify and access a Java key store.
 * <p>
 * Instances hold the keystore name, alias, password, and resolved file location so callers
 * can pass the information together when creating or loading keystores.
 */
public class KeyStoreInfo {

  private Path keystorePath;
  private String keystoreName;
  private String keystoreAlias;
  private String keystorePassword;

  /**
   * Creates a keystore descriptor with only a file path. Other properties can be set later
   * via the mutators or populated by helper utilities.
   *
   * @param keystorePath the path where the keystore resides or should be created
   */
  public KeyStoreInfo (Path keystorePath) {

    this.keystorePath = keystorePath;
  }

  /**
   * Creates a fully populated keystore descriptor.
   *
   * @param keystoreName     the keystore file name
   * @param keystoreAlias    the alias to assign when storing the key and certificate chain
   * @param keystorePassword the password protecting the keystore
   * @param keystorePath     the resolved path of the keystore file
   */
  public KeyStoreInfo (String keystoreName, String keystoreAlias, String keystorePassword, Path keystorePath) {

    this.keystoreName = keystoreName;
    this.keystoreAlias = keystoreAlias;
    this.keystorePassword = keystorePassword;
    this.keystorePath = keystorePath;
  }

  /**
   * Retrieves the keystore file name (without path).
   *
   * @return the keystore name, or {@code null} if not set
   */
  public String getKeystoreName () {

    return keystoreName;
  }

  /**
   * Sets the keystore file name.
   *
   * @param keystoreName the file name to use
   */
  public void setKeystoreName (String keystoreName) {

    this.keystoreName = keystoreName;
  }

  /**
   * Retrieves the alias used for the stored key and certificate chain.
   *
   * @return the configured alias, or {@code null} if none has been set
   */
  public String getKeystoreAlias () {

    return keystoreAlias;
  }

  /**
   * Sets the alias used for entries in the keystore.
   *
   * @param keystoreAlias the alias to assign
   */
  public void setKeystoreAlias (String keystoreAlias) {

    this.keystoreAlias = keystoreAlias;
  }

  /**
   * Retrieves the password protecting the keystore.
   *
   * @return the keystore password, or {@code null} if not set
   */
  public String getKeystorePassword () {

    return keystorePassword;
  }

  /**
   * Sets the password used to protect the keystore.
   *
   * @param keystorePassword the password value to store
   */
  public void setKeystorePassword (String keystorePassword) {

    this.keystorePassword = keystorePassword;
  }

  /**
   * Retrieves the resolved filesystem path of the keystore.
   *
   * @return the keystore path
   */
  public Path getKeystorePath () {

    return keystorePath;
  }

  /**
   * Sets the filesystem path of the keystore file.
   *
   * @param keystorePath the path to the keystore on disk
   */
  public void setKeystorePath (Path keystorePath) {

    this.keystorePath = keystorePath;
  }
}
