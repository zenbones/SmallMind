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
 * Spring factory bean that expands a server pattern/spread into an array of {@link RabbitMQServer} instances.
 */
public class RabbitMQServerFactoryBean implements FactoryBean<RabbitMQServer[]>, InitializingBean {

  private RabbitMQServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  /**
   * Pattern describing hosts (and optional ports). Use '#' as a placeholder to be replaced by spread values.
   * <p>
   * Examples:
   * host-#.example.com:5672 with spread "1-3" yields host-1.example.com:5672 ... host-3.example.com:5672
   *
   * @param serverPattern host/port pattern.
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Spread string parsed by {@link Spread#calculate(String)} to generate placeholder substitutions.
   *
   * @param serverSpread spread definition (e.g., "1-3,5").
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Parses the pattern/spread and creates the server array.
   *
   * @throws SpreadParserException if the spread cannot be parsed.
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
   * @return generated array of {@link RabbitMQServer} entries.
   */
  @Override
  public RabbitMQServer[] getObject () {

    return serverArray;
  }

  /**
   * @return the object type produced by this factory.
   */
  @Override
  public Class<?> getObjectType () {

    return RabbitMQServer[].class;
  }

  /**
   * @return true because this factory produces a singleton array instance.
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
