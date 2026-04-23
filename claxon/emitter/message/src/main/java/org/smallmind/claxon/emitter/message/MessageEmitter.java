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
package org.smallmind.claxon.emitter.message;

import java.util.function.Consumer;
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * Push emitter that formats Claxon metrics into human-readable strings and forwards them to a
 * configurable {@link Consumer}{@code <String>}.
 *
 * <p>Each quantity produces one message of the form
 * {@code meterName[key1=val1, key2=val2].quantityName=value}. By default messages are written
 * to standard output, but any {@link Consumer}{@code <String>} may be supplied — for example a
 * logging adapter such as {@link ScribeLoggerConsumer}.
 */
public class MessageEmitter extends PushEmitter {

  /**
   * The consumer to which each formatted metric message is delivered.
   */
  private final Consumer<String> output;

  /**
   * Creates a message emitter that prints formatted metric lines to {@link System#out}.
   */
  public MessageEmitter () {

    this(System.out::println);
  }

  /**
   * Creates a message emitter that delivers formatted metric lines to the given consumer.
   *
   * @param output the {@link Consumer}{@code <String>} that will receive each formatted metric
   *               line; must not be {@code null}
   */
  public MessageEmitter (Consumer<String> output) {

    this.output = output;
  }

  /**
   * Formats each quantity into a metric string and passes it to the configured output consumer.
   *
   * <p>The format of each emitted string is:
   * <pre>{@code meterName[key1=val1, key2=val2].quantityName=value}</pre>
   * When there are no tags, the bracket section is empty: {@code meterName[].quantityName=value}.
   *
   * @param meterName  the base name of the meter
   * @param tags       the tags to include in the formatted string; may be {@code null} or empty
   * @param quantities the measured values to format and emit; must not be {@code null}
   */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    StringBuilder recordBuilder = new StringBuilder(meterName);

    recordBuilder.append('[');
    if ((tags != null) && (tags.length > 0)) {

      boolean first = true;

      for (Tag tag : tags) {
        if (!first) {
          recordBuilder.append(", ");
        }
        recordBuilder.append(tag.getKey()).append("=").append(tag.getValue());
        first = false;
      }
    }
    recordBuilder.append("].");

    for (Quantity quantity : quantities) {
      output.accept(recordBuilder + quantity.getName() + "=" + quantity.getValue());
    }
  }
}
