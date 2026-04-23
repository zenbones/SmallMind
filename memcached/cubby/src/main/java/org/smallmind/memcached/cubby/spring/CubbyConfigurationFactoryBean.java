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
package org.smallmind.memcached.cubby.spring;

import org.smallmind.memcached.cubby.Authentication;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.locator.KeyLocator;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that assembles a {@link CubbyConfiguration} from a named preset
 * and a set of optional property overrides.
 *
 * <p>The baseline configuration is selected via {@link #setInitial(CubbyConfigurations)}, which
 * defaults to {@link CubbyConfigurations#DEFAULT}. Any non-null property injected through the
 * setter methods overrides the corresponding field on the preset configuration after
 * {@link #afterPropertiesSet()} is called.</p>
 *
 * <p>This bean is always singleton-scoped.</p>
 */
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

  /**
   * Reports that this factory bean always returns the same singleton instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the concrete type produced by this factory.
   *
   * @return {@link CubbyConfiguration}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return CubbyConfiguration.class;
  }

  /**
   * Returns the assembled {@link CubbyConfiguration}.
   *
   * @return the configuration built during {@link #afterPropertiesSet()}
   */
  @Override
  public CubbyConfiguration getObject () {

    return configuration;
  }

  /**
   * Selects the named preset used as the baseline for the assembled configuration.
   *
   * @param initial the preset to start from; defaults to {@link CubbyConfigurations#DEFAULT}
   */
  public void setInitial (CubbyConfigurations initial) {

    this.initial = initial;
  }

  /**
   * Overrides the codec used to serialise and deserialise cached values.
   *
   * @param codec the codec implementation to apply
   */
  public void setCodec (CubbyCodec codec) {

    this.codec = codec;
  }

  /**
   * Overrides the strategy used to select which server holds a given key.
   *
   * @param keyLocator the key-locator implementation to apply
   */
  public void setKeyLocator (KeyLocator keyLocator) {

    this.keyLocator = keyLocator;
  }

  /**
   * Overrides the strategy used to translate cache keys into memcached-safe strings.
   *
   * @param keyTranslator the key-translator implementation to apply
   */
  public void setKeyTranslator (KeyTranslator keyTranslator) {

    this.keyTranslator = keyTranslator;
  }

  /**
   * Supplies SASL authentication credentials used when connecting to secured servers.
   *
   * @param authentication the username/password credentials to apply
   */
  public void setAuthentication (Authentication authentication) {

    this.authentication = authentication;
  }

  /**
   * Overrides the maximum time a client operation will wait for a server response.
   *
   * @param defaultRequestTimeoutMilliseconds timeout in milliseconds
   */
  public void setDefaultRequestTimeoutMilliseconds (Long defaultRequestTimeoutMilliseconds) {

    this.defaultRequestTimeoutMilliseconds = defaultRequestTimeoutMilliseconds;
  }

  /**
   * Overrides the maximum time to wait when establishing a new TCP connection.
   *
   * @param connectionTimeoutMilliseconds timeout in milliseconds
   */
  public void setConnectionTimeoutMilliseconds (Long connectionTimeoutMilliseconds) {

    this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
  }

  /**
   * Overrides the interval between keep-alive NOOP commands sent to idle connections.
   *
   * @param keepAliveSeconds interval in seconds
   */
  public void setKeepAliveSeconds (Long keepAliveSeconds) {

    this.keepAliveSeconds = keepAliveSeconds;
  }

  /**
   * Overrides the delay before attempting to reconnect a host after a connection failure.
   *
   * @param resuscitationSeconds delay in seconds
   */
  public void setResuscitationSeconds (Long resuscitationSeconds) {

    this.resuscitationSeconds = resuscitationSeconds;
  }

  /**
   * Overrides the number of concurrent TCP connections maintained per memcached host.
   *
   * @param connectionsPerHost the desired connection-pool size per host
   */
  public void setConnectionsPerHost (Integer connectionsPerHost) {

    this.connectionsPerHost = connectionsPerHost;
  }

  /**
   * Assembles the {@link CubbyConfiguration} by applying all non-null property overrides to the
   * configuration obtained from the selected {@link CubbyConfigurations} preset.
   */
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
