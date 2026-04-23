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
 * Holds the name, alias, password, and filesystem path needed to identify and access a Java keystore.
 */
public class KeyStoreInfo {

  private Path keystorePath;
  private String keystoreName;
  private String keystoreAlias;
  private String keystorePassword;

  /**
   * Creates a keystore descriptor with only a filesystem path; all other properties may be set later via the mutators.
   *
   * @param keystorePath the path where the keystore resides or should be created
   */
  public KeyStoreInfo (Path keystorePath) {

    this.keystorePath = keystorePath;
  }

  /**
   * Creates a fully populated keystore descriptor with all properties set at construction time.
   *
   * @param keystoreName     the keystore file name
   * @param keystoreAlias    the alias under which the key and certificate chain are stored
   * @param keystorePassword the password protecting the keystore
   * @param keystorePath     the resolved filesystem path of the keystore file
   */
  public KeyStoreInfo (String keystoreName, String keystoreAlias, String keystorePassword, Path keystorePath) {

    this.keystoreName = keystoreName;
    this.keystoreAlias = keystoreAlias;
    this.keystorePassword = keystorePassword;
    this.keystorePath = keystorePath;
  }

  /**
   * Returns the keystore file name, or {@code null} if not set.
   *
   * @return the keystore file name
   */
  public String getKeystoreName () {

    return keystoreName;
  }

  /**
   * Sets the keystore file name.
   *
   * @param keystoreName the file name to assign
   */
  public void setKeystoreName (String keystoreName) {

    this.keystoreName = keystoreName;
  }

  /**
   * Returns the alias used for entries in the keystore, or {@code null} if not set.
   *
   * @return the keystore alias
   */
  public String getKeystoreAlias () {

    return keystoreAlias;
  }

  /**
   * Sets the alias used for entries in the keystore.
   *
   * @param keystoreAlias the alias value to assign
   */
  public void setKeystoreAlias (String keystoreAlias) {

    this.keystoreAlias = keystoreAlias;
  }

  /**
   * Returns the password protecting the keystore, or {@code null} if not set.
   *
   * @return the keystore password
   */
  public String getKeystorePassword () {

    return keystorePassword;
  }

  /**
   * Sets the password used to protect the keystore.
   *
   * @param keystorePassword the password to assign
   */
  public void setKeystorePassword (String keystorePassword) {

    this.keystorePassword = keystorePassword;
  }

  /**
   * Returns the resolved filesystem path of the keystore file.
   *
   * @return the keystore path
   */
  public Path getKeystorePath () {

    return keystorePath;
  }

  /**
   * Sets the filesystem path of the keystore file.
   *
   * @param keystorePath the path to assign
   */
  public void setKeystorePath (Path keystorePath) {

    this.keystorePath = keystorePath;
  }
}
