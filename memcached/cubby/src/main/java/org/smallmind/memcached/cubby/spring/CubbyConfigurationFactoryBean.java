/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.memcached.cubby.spring;

import org.smallmind.memcached.cubby.Authentication;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.locator.KeyLocator;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class CubbyConfigurationFactoryBean implements FactoryBean<CubbyConfiguration>, InitializingBean {

  private CubbyConfiguration configuration;
  private CubbyConfigurations initial = CubbyConfigurations.DEFAULT;
  private CubbyCodec codec;
  private KeyLocator keyLocator;
  private KeyTranslator keyTranslator;
  private Authentication authentication;
  private Long defaultRequestTimeoutMilliseconds;
  private Long connectionTimeoutMilliseconds;
  private Long keepAliveSeconds;
  private Long resuscitationSeconds;
  private Integer connectionsPerHost;

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return CubbyConfiguration.class;
  }

  @Override
  public CubbyConfiguration getObject () {

    return configuration;
  }

  public void setInitial (CubbyConfigurations initial) {

    this.initial = initial;
  }

  public void setCodec (CubbyCodec codec) {

    this.codec = codec;
  }

  public void setKeyLocator (KeyLocator keyLocator) {

    this.keyLocator = keyLocator;
  }

  public void setKeyTranslator (KeyTranslator keyTranslator) {

    this.keyTranslator = keyTranslator;
  }

  public void setAuthentication (Authentication authentication) {

    this.authentication = authentication;
  }

  public void setDefaultRequestTimeoutMilliseconds (Long defaultRequestTimeoutMilliseconds) {

    this.defaultRequestTimeoutMilliseconds = defaultRequestTimeoutMilliseconds;
  }

  public void setConnectionTimeoutMilliseconds (Long connectionTimeoutMilliseconds) {

    this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
  }

  public void setKeepAliveSeconds (Long keepAliveSeconds) {

    this.keepAliveSeconds = keepAliveSeconds;
  }

  public void setResuscitationSeconds (Long resuscitationSeconds) {

    this.resuscitationSeconds = resuscitationSeconds;
  }

  public void setConnectionsPerHost (Integer connectionsPerHost) {

    this.connectionsPerHost = connectionsPerHost;
  }

  @Override
  public void afterPropertiesSet () {

    configuration = initial.getConfiguration();

    if (codec != null) {
      configuration.setCodec(codec);
    }
    if (keyLocator != null) {
      configuration.setKeyLocator(keyLocator);
    }
    if (keyTranslator != null) {
      configuration.setKeyTranslator(keyTranslator);
    }
    if (authentication != null) {
      configuration.setAuthentication(authentication);
    }
    if (defaultRequestTimeoutMilliseconds != null) {
      configuration.setDefaultRequestTimeoutMilliseconds(defaultRequestTimeoutMilliseconds);
    }
    if (connectionTimeoutMilliseconds != null) {
      configuration.setConnectionTimeoutMilliseconds(connectionTimeoutMilliseconds);
    }
    if (keepAliveSeconds != null) {
      configuration.setKeepAliveSeconds(keepAliveSeconds);
    }
    if (resuscitationSeconds != null) {
      configuration.setResuscitationSeconds(resuscitationSeconds);
    }
    if (connectionsPerHost != null) {
      configuration.setConnectionsPerHost(connectionsPerHost);
    }
  }
}
