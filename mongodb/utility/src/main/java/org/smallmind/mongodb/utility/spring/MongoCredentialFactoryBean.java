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
package org.smallmind.mongodb.utility.spring;

import com.mongodb.MongoCredential;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that produces {@link MongoCredential} instances from configured username, password, and source.
 */
public class MongoCredentialFactoryBean implements InitializingBean, FactoryBean<MongoCredential> {

  private MongoCredential mongoCredential;
  private String user;
  private String password;
  private String source;

  /**
   * Sets the username used for SCRAM-SHA-1 authentication.
   *
   * @param user the authentication username
   */
  public void setUser (String user) {

    this.user = user;
  }

  /**
   * Sets the password used for SCRAM-SHA-1 authentication.
   *
   * @param password the authentication password
   */
  public void setPassword (String password) {

    this.password = password;
  }

  /**
   * Sets the name of the authentication source database.
   *
   * @param source the database against which the credential is authenticated
   */
  public void setSource (String source) {

    this.source = source;
  }

  /**
   * Returns {@code true}; the constructed credential is a shared singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns {@code MongoCredential.class}.
   *
   * @return {@code MongoCredential.class}
   */
  @Override
  public Class<?> getObjectType () {

    return MongoCredential.class;
  }

  /**
   * Returns the constructed {@link MongoCredential}.
   *
   * @return the credential built during {@code afterPropertiesSet}
   */
  @Override
  public MongoCredential getObject () {

    return mongoCredential;
  }

  /**
   * Builds the credential after properties have been set.
   */
  @Override
  public void afterPropertiesSet () {

    mongoCredential = MongoCredential.createScramSha1Credential(user, source, password.toCharArray());
  }
}
