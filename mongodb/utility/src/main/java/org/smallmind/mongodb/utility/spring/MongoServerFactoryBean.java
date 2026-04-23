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

import java.util.LinkedList;
import com.mongodb.ServerAddress;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that constructs an array of {@link ServerAddress} instances from template patterns or spreads.
 */
public class MongoServerFactoryBean implements InitializingBean, FactoryBean<ServerAddress[]> {

  private ServerAddress[] serverAddresses;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the server pattern string used to generate host names, optionally containing {@code '#'} placeholders
   * for spread expansion.
   *
   * @param serverPattern the pattern, e.g. {@code "mongo#.example.com:27017"} or a plain host name
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread expression whose values replace {@code '#'} placeholders in the server pattern.
   *
   * @param serverSpread the spread expression, e.g. {@code "1-3"} to expand into {@code 1}, {@code 2}, {@code 3}
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Returns {@code true}; the built server address array is a shared singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns {@code ServerAddress[].class}.
   *
   * @return {@code ServerAddress[].class}
   */
  @Override
  public Class<?> getObjectType () {

    return ServerAddress[].class;
  }

  /**
   * Returns the array of configured server addresses.
   *
   * @return the server address array built during {@code afterPropertiesSet}
   */
  @Override
  public ServerAddress[] getObject () {

    return serverAddresses;
  }

  /**
   * Parses the configured pattern and spread to produce the array of server addresses.
   *
   * @throws SpreadParserException if the spread expression cannot be parsed
   */
  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    if ((serverPattern != null) && (serverPattern.length() > 0)) {

      LinkedList<ServerAddress> serverList = new LinkedList<>();
      int colonPos = serverPattern.indexOf(':');
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        if (colonPos >= 0) {
          serverList.add(new ServerAddress(serverPattern.substring(0, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        } else {
          serverList.add(new ServerAddress(serverPattern, 27017));
        }
      } else {
        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (colonPos >= 0) {
            serverList.add(new ServerAddress(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
          } else {
            serverList.add(new ServerAddress(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1), 27017));
          }
        }
      }

      serverAddresses = new ServerAddress[serverList.size()];
      serverList.toArray(serverAddresses);
    }
  }
}
