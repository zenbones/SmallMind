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
package org.smallmind.kafka.utility;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.Spread;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that materializes an array of {@link KafkaServer} instances from either a
 * single explicit host/port or a spread pattern (e.g., {@code kafka-#}.example.com).
 * This allows configuring multiple brokers via a compact property string.
 */
public class KafkaServerFactoryBean implements FactoryBean<KafkaServer[]>, InitializingBean {

  private KafkaServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the pattern representing broker addresses. A {@code #} character will be replaced with
   * values from the configured spread, and an optional port may follow a colon.
   *
   * @param serverPattern pattern describing the brokers
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread expression used to expand {@code serverPattern} when it contains {@code #}.
   *
   * @param serverSpread spread descriptor understood by {@link Spread#calculate(String)}
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Always produces a singleton array.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Declares the object type managed by this factory.
   *
   * @return {@link KafkaServer} array class
   */
  @Override
  public Class<?> getObjectType () {

    return KafkaServer[].class;
  }

  /**
   * Returns the broker array constructed during initialization.
   *
   * @return configured {@link KafkaServer} array or {@code null} if not initialized
   */
  @Override
  public KafkaServer[] getObject () {

    return serverArray;
  }

  /**
   * Parses the provided pattern and spread into an array of {@link KafkaServer} objects.
   *
   * @throws SpreadParserException if the spread expression cannot be parsed
   */
  @Override
  public void afterPropertiesSet ()
    throws SpreadParserException {

    if ((serverPattern != null) && (!serverPattern.isEmpty())) {

      LinkedList<KafkaServer> serverList = new LinkedList<>();
      int colonPos = serverPattern.indexOf(':');
      int poundPos;

      if ((poundPos = serverPattern.indexOf('#')) < 0) {
        if (colonPos >= 0) {
          serverList.add(new KafkaServer(serverPattern.substring(0, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
        } else {
          serverList.add(new KafkaServer(serverPattern, 9092));
        }
      } else {
        for (String serverDesignator : Spread.calculate(serverSpread)) {
          if (colonPos >= 0) {
            serverList.add(new KafkaServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1, colonPos), Integer.parseInt(serverPattern.substring(colonPos + 1))));
          } else {
            serverList.add(new KafkaServer(serverPattern.substring(0, poundPos) + serverDesignator + serverPattern.substring(poundPos + 1), 9092));
          }
        }
      }

      serverArray = new KafkaServer[serverList.size()];
      serverList.toArray(serverArray);
    }
  }
}
