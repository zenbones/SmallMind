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
 * Spring {@link FactoryBean} that expands a server-pattern string and an optional spread
 * specification into an ordered array of {@link MemcachedServer} instances.
 *
 * <p>The pattern syntax mirrors that of
 * {@link org.smallmind.memcached.utility.spring.MemcachedServerMapFactoryBean} and supports two
 * forms:</p>
 * <ul>
 *   <li><em>Single server</em> &ndash; a plain {@code host} or {@code host:port} string with no
 *       {@code #} token. A single-element array is produced.</li>
 *   <li><em>Multiple servers via spread</em> &ndash; a pattern containing {@code #} as a
 *       placeholder. The spread is expanded using {@link Spread#calculate(String)} and each
 *       resulting designator replaces {@code #} in the pattern. Servers appear in the array in
 *       the order produced by the spread.</li>
 * </ul>
 *
 * <p>When no explicit port is included in the pattern the default memcached port {@code 11211}
 * is used. This bean is always singleton-scoped.</p>
 */
public class MemcachedServerFactoryBean implements FactoryBean<MemcachedServer[]>, InitializingBean {

  private MemcachedServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the server pattern used to derive hostnames and optional port numbers.
   *
   * <p>May contain a single {@code #} placeholder that is substituted by each spread value.
   * A colon-separated port suffix may appear after the host portion or after the {@code #} token.</p>
   *
   * @param serverPattern the pattern string (e.g., {@code "cache#.example.com:11211"})
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread specification applied to {@code #} placeholders in the pattern.
   *
   * <p>The value is passed directly to {@link Spread#calculate(String)} and may use range
   * notation such as {@code "1-3"} to expand to designators {@code 1}, {@code 2}, {@code 3}.</p>
   *
   * @param serverSpread the spread string (e.g., {@code "1-3"})
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

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
   * @return {@link MemcachedServer}{@code [].class}
   */
  @Override
  public Class<?> getObjectType () {

    return MemcachedServer[].class;
  }

  /**
   * Returns the array of {@link MemcachedServer} instances assembled during
   * {@link #afterPropertiesSet()}, or {@code null} if the pattern was empty or not set.
   *
   * @return the server array, or {@code null}
   */
  @Override
  public MemcachedServer[] getObject () {

    return serverArray;
  }

  /**
   * Parses the configured pattern and spread to build the {@link MemcachedServer} array.
   *
   * <p>This method is called automatically by the Spring container after all properties have been
   * injected. If {@code serverPattern} is null or empty, the array remains {@code null}.</p>
   *
   * @throws SpreadParserException if the spread specification is syntactically invalid
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
