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
 * Spring {@link FactoryBean} that produces a {@link KafkaServer}{@code []} from a compact
 * pattern string, optionally expanded via a spread expression.
 *
 * <p>The pattern may be a plain {@code host:port} pair, a bare hostname (defaulting to port
 * {@code 9092}), or a template containing {@code #} which is replaced by each value produced
 * by the spread — e.g. pattern {@code kafka-#.example.com:9092} with spread {@code 1-3} yields
 * three brokers: {@code kafka-1.example.com:9092}, {@code kafka-2.example.com:9092}, and
 * {@code kafka-3.example.com:9092}.
 */
public class KafkaServerFactoryBean implements FactoryBean<KafkaServer[]>, InitializingBean {

  private KafkaServer[] serverArray;
  private String serverPattern;
  private String serverSpread;

  /**
   * Sets the broker address pattern.  A {@code #} placeholder is substituted with each value
   * from the spread; an optional {@code :port} suffix overrides the default port {@code 9092}.
   *
   * @param serverPattern address pattern, e.g. {@code kafka-#.internal:9094} or {@code broker.example.com:9094}
   */
  public void setServerPattern (String serverPattern) {

    this.serverPattern = serverPattern;
  }

  /**
   * Sets the spread expression used to expand a pattern that contains {@code #}.
   * The value is interpreted by {@link Spread#calculate(String)}.
   *
   * @param serverSpread spread descriptor such as {@code 1-3} or {@code a,b,c}
   */
  public void setServerSpread (String serverSpread) {

    this.serverSpread = serverSpread;
  }

  /**
   * Indicates that this factory always returns the same array instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the managed object type.
   *
   * @return {@code KafkaServer[].class}
   */
  @Override
  public Class<?> getObjectType () {

    return KafkaServer[].class;
  }

  /**
   * Returns the broker array assembled by {@link #afterPropertiesSet()}.
   *
   * @return configured {@link KafkaServer} array, or {@code null} before initialization
   */
  @Override
  public KafkaServer[] getObject () {

    return serverArray;
  }

  /**
   * Parses {@code serverPattern} and, when it contains {@code #}, expands it with
   * {@code serverSpread} to produce one {@link KafkaServer} per spread value.
   * Patterns without {@code #} yield a single-element array.
   *
   * @throws SpreadParserException if {@code serverSpread} is set but cannot be parsed
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
