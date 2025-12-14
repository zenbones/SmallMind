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
package org.smallmind.memcached.utility.spring;

import java.util.LinkedList;
import org.smallmind.memcached.utility.MemcachedServer;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring FactoryBean that expands server patterns into an array of {@link MemcachedServer} instances.
 */
public class MemcachedServerFactoryBean implements FactoryBean<MemcachedServer[]>, InitializingBean {

  private MemcachedServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the server pattern (supports # placeholders and optional host:port).
   *
   * @param serverPattern pattern string
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread specification used to replace # placeholders.
   *
   * @param serverSpread spread value (e.g., 1-3)
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Indicates that this factory bean is a singleton.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the produced object type.
   *
   * @return {@link MemcachedServer} array class
   */
  @Override
  public Class<?> getObjectType () {

    return MemcachedServer[].class;
  }

  /**
   * Provides the configured {@link MemcachedServer} array.
   *
   * @return array of servers, or {@code null} if not yet initialized
   */
  @Override
  public MemcachedServer[] getObject () {

    return serverArray;
  }

  /**
   * Parses the pattern/spread settings to create the server array.
   *
   * <p>Expands `#` tokens using the spread and applies default port 11211 when none is provided.</p>
   *
   * @throws SpreadParserException if the spread cannot be parsed
   */
  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    if ((serverPattern != null) && (!serverPattern.isEmpty())) {

      LinkedList<MemcachedServer> serverList = new LinkedList<>();
      int colonPos = serverPattern.indexOf(':');
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        if (colonPos >= 0) {
          serverList.add(new MemcachedServer(serverPattern.substring(0, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        } else {
          serverList.add(new MemcachedServer(serverPattern, 11211));
        }
      } else {
        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (colonPos >= 0) {
            serverList.add(new MemcachedServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
          } else {
            serverList.add(new MemcachedServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1), 11211));
          }
        }
      }

      serverArray = new MemcachedServer[serverList.size()];
      serverList.toArray(serverArray);
    }
  }
}
