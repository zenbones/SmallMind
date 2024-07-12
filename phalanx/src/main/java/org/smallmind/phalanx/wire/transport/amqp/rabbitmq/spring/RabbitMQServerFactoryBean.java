/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class RabbitMQServerFactoryBean implements FactoryBean<RabbitMQServer[]>, InitializingBean {

  private RabbitMQServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

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

  @Override
  public RabbitMQServer[] getObject () {

    return serverArray;
  }

  @Override
  public Class<?> getObjectType () {

    return RabbitMQServer[].class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
