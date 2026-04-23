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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq.spring;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that expands a host pattern and numeric spread into an array of {@link RabbitMQServer} instances.
 */
public class RabbitMQServerFactoryBean implements FactoryBean<RabbitMQServer[]>, InitializingBean {

  private RabbitMQServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the host pattern used to generate server addresses.
   * Use {@code #} as a placeholder for values produced by the spread.
   * Include an optional {@code :port} suffix to specify a non-default port.
   * Example: {@code broker-#.example.com:5672} with spread {@code "1-3"} produces three servers.
   *
   * @param serverPattern host/port pattern string.
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread string used to generate placeholder substitutions for the server pattern.
   * Parsed by {@link Spread#calculate(String)}; examples: {@code "1-3"}, {@code "1,3,5"}.
   *
   * @param serverSpread spread definition string.
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Builds the server array by expanding the pattern with each value from the spread.
   *
   * @throws SpreadParserException if the spread string cannot be parsed.
   */
  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    if ((serverPattern != null) && (serverPattern.length() > 0)) {

      LinkedList<RabbitMQServer> serverList = new LinkedList<>();
      int colonPos = serverPattern.indexOf(':');
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        if (colonPos >= 0) {
          serverList.add(new RabbitMQServer(serverPattern.substring(0, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        } else {
          serverList.add(new RabbitMQServer(serverPattern, 5672));
        }
      } else {
        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (colonPos >= 0) {
            serverList.add(new RabbitMQServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
          } else {
            serverList.add(new RabbitMQServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1), 5672));
          }
        }
      }

      serverArray = new RabbitMQServer[serverList.size()];
      serverList.toArray(serverArray);
    }
  }

  /**
   * Returns the generated array of {@link RabbitMQServer} entries.
   *
   * @return server array built from the pattern and spread.
   */
  @Override
  public RabbitMQServer[] getObject () {

    return serverArray;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link RabbitMQServer} array class.
   */
  @Override
  public Class<?> getObjectType () {

    return RabbitMQServer[].class;
  }

  /**
   * Returns whether this factory produces a singleton.
   *
   * @return {@code true} because the array instance is created once and reused.
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
