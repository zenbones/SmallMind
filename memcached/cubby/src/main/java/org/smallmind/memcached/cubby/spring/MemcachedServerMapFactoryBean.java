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

import java.util.HashMap;
import java.util.Map;
import org.smallmind.memcached.utility.MemcachedServer;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring FactoryBean that parses server patterns into a map of named {@link MemcachedServer} instances.
 */
public class MemcachedServerMapFactoryBean implements FactoryBean<Map<String, MemcachedServer>>, InitializingBean {

  private Map<String, MemcachedServer> serverMap;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the server pattern (supports # for spreads and optional host:port).
   *
   * @param serverPattern pattern string
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread specification applied to # placeholders.
   *
   * @param serverSpread spread value (e.g., 1-3)
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Parses the configured pattern and spread into a map of {@link MemcachedServer} instances.
   *
   * <p>Expands {@code #} tokens via the spread and defaults the port to 11211 when unspecified.</p>
   *
   * @throws SpreadParserException if the spread cannot be parsed
   */
  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    serverMap = new HashMap<>();

    if ((serverPattern != null) && (serverPattern.length() > 0)) {

      int colonPos = serverPattern.indexOf(':');
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        if (colonPos >= 0) {
          serverMap.put("memcached", new MemcachedServer(serverPattern.substring(0, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        } else {
          serverMap.put("memcached", new MemcachedServer(serverPattern, 11211));
        }
      } else {
        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (colonPos >= 0) {
            serverMap.put(serverDesignator, new MemcachedServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
          } else {
            serverMap.put(serverDesignator, new MemcachedServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1), 11211));
          }
        }
      }
    }
  }

  /**
   * Returns the built server map.
   *
   * @return map of server name to {@link MemcachedServer}
   */
  @Override
  public Map<String, MemcachedServer> getObject () {

    return serverMap;
  }

  /**
   * Declares the object type produced by this factory.
   *
   * @return {@link Map} class
   */
  @Override
  public Class<?> getObjectType () {

    return Map.class;
  }

  /**
   * Indicates this factory bean is a singleton.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
