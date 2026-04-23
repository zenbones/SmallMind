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
package org.smallmind.claxon.emitter.datadog;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.claxon.registry.Tag;

/**
 * Push emitter that forwards Claxon metrics to Datadog via a non-blocking StatsD client.
 *
 * <p>Quantities of type {@link QuantityType#COUNT} can optionally be sent as StatsD counters
 * (via {@link StatsDClient#count}); all other quantities are sent as gauges. Tags are
 * translated from the Claxon {@code key=value} format to the Datadog {@code key:value} format
 * expected by the StatsD protocol.
 */
public class DataDogEmitter extends PushEmitter {

  /**
   * The underlying non-blocking StatsD client used to transmit metrics to the Datadog agent.
   */
  private final StatsDClient statsdClient;

  /**
   * When {@code true}, quantities whose type is {@link QuantityType#COUNT} are emitted as
   * StatsD counters rather than gauges.
   */
  private final boolean countAsCount;

  /**
   * Creates a Datadog emitter with default connection settings: no prefix, {@code localhost},
   * port {@code 8125}, and count-as-count behaviour enabled.
   */
  public DataDogEmitter () {

    this(null, "localhost", 8125, true, null);
  }

  /**
   * Creates a Datadog emitter with fully configurable connection parameters and constant tags.
   *
   * @param prefix       optional metric name prefix prepended to every metric; may be
   *                     {@code null}
   * @param hostName     hostname of the Datadog StatsD agent
   * @param port         UDP port of the Datadog StatsD agent
   * @param countAsCount {@code true} to send {@link QuantityType#COUNT} quantities as StatsD
   *                     counters; {@code false} to send them as gauges
   * @param constantTags tags attached to every metric emission; may be {@code null} or empty
   */
  public DataDogEmitter (String prefix, String hostName, int port, boolean countAsCount, Tag... constantTags) {

    this.countAsCount = countAsCount;

    statsdClient = new NonBlockingStatsDClientBuilder().prefix(prefix).hostname(hostName).port(port).constantTags(translateTags(constantTags)).build();
  }

  /**
   * Sends each quantity to Datadog as either a counter or a gauge, with per-call tags attached.
   *
   * <p>The metric name submitted to Datadog is formed by joining {@code meterName} and
   * {@code quantity.getName()} with a period. If {@link #countAsCount} is {@code true} and
   * the quantity's type is {@link QuantityType#COUNT}, {@link StatsDClient#count} is used;
   * otherwise {@link StatsDClient#gauge} is used.
   *
   * @param meterName  the base name of the meter; combined with each quantity name to form
   *                   the Datadog metric name
   * @param tags       per-emission tags translated to Datadog {@code key:value} format; may
   *                   be {@code null} or empty
   * @param quantities the measured values to emit; must not be {@code null}
   */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    String[] translatedTags = translateTags(tags);

    for (Quantity quantity : quantities) {
      if (countAsCount && QuantityType.COUNT.equals(quantity.getType())) {
        statsdClient.count(meterName + '.' + quantity.getName(), quantity.getValue(), translatedTags);
      } else {
        statsdClient.gauge(meterName + '.' + quantity.getName(), quantity.getValue(), translatedTags);
      }
    }
  }

  /**
   * Converts an array of Claxon {@link Tag} objects into the {@code key:value} string array
   * format expected by the Datadog StatsD client.
   *
   * @param tags the tags to translate; may be {@code null} or empty
   * @return a {@code String[]} in Datadog format, or {@code null} when the input is empty
   */
  private String[] translateTags (Tag... tags) {

    String[] translatedtags = null;

    if ((tags != null) && (tags.length > 0)) {

      int index = 0;

      translatedtags = new String[tags.length];
      for (Tag tag : tags) {
        translatedtags[index++] = tag.getKey() + ':' + tag.getValue();
      }
    }

    return translatedtags;
  }
}
