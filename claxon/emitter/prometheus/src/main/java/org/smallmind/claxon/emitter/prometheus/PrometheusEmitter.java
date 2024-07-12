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
package org.smallmind.claxon.emitter.prometheus;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.claxon.registry.PullEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class PrometheusEmitter extends PullEmitter<String> {

  private enum Letter {UPPER, LOWER, DIGIT, PUNCTUATION, UNKNOWN}

  private ConcurrentHashMap<PrometheusKey, Double> readMap = new ConcurrentHashMap<>();
  private volatile ConcurrentHashMap<PrometheusKey, Double> writeMap = new ConcurrentHashMap<>();

  /*
  # HELP metric_name Description of the metric
  # TYPE metric_name type
  # Comment that's not parsed by prometheus
  http_requests_total{method="post",code="400"}  3   1395066363000
  */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    for (Quantity quantity : quantities) {
      writeMap.put(new PrometheusKey(meterName, tags, quantity.getName()), quantity.getValue());
    }
  }

  @Override
  public synchronized String emit () {

    StringBuilder outputBuilder = new StringBuilder();
    ConcurrentHashMap<PrometheusKey, Double> tempMap;

    tempMap = readMap;
    readMap = writeMap;
    writeMap = tempMap;

    for (Map.Entry<PrometheusKey, Double> traceEntry : readMap.entrySet()) {
      format(outputBuilder, traceEntry.getKey()).append(' ').append(fromDouble(traceEntry.getValue())).append('\n');
    }

    outputBuilder.append("# EOF\n");
    readMap.clear();

    return outputBuilder.toString();
  }

  private String fromDouble (double value) {

    if (value == Double.POSITIVE_INFINITY) {
      return "+Inf";
    } else if (value == Double.NEGATIVE_INFINITY) {
      return "-Inf";
    } else if (Double.isNaN(value)) {
      return "NaN";
    } else if (value == 0.0D) {
      return "0";
    } else {
      return Double.toString(value);
    }
  }

  private StringBuilder format (StringBuilder outputBuilder, PrometheusKey prometheusKey) {

    mangle(outputBuilder, prometheusKey.getMeterName()).append(':');
    mangle(outputBuilder, prometheusKey.getQuantityName());

    if ((prometheusKey.getTags() != null) && (prometheusKey.getTags().length > 0)) {

      boolean first = true;

      outputBuilder.append('{');
      for (Tag tag : prometheusKey.getTags()) {
        if (!first) {
          outputBuilder.append(',');
        }

        mangle(outputBuilder, tag.getKey()).append("=\"").append(tag.getValue()).append('"');
        first = false;
      }
      outputBuilder.append("}");
    }

    return outputBuilder;
  }

  // Being Golang, Prometheus can't handle unicode strings like most frameworks, but only a very simple set of characters.
  private StringBuilder mangle (StringBuilder outputBuilder, String original) {

    Letter state = Letter.UNKNOWN;

    for (int index = 0; index < original.length(); index++) {

      Letter priorState = state;
      char singleChar = original.charAt(index);

      if ((singleChar >= 'a') && (singleChar <= 'z')) {
        state = Letter.LOWER;
      } else if ((singleChar >= 'A') && (singleChar <= 'Z')) {
        state = Letter.UPPER;
      } else if ((singleChar >= '0') && (singleChar <= '9')) {
        if (index == 0) {
          outputBuilder.append('_');
        }
        state = Letter.DIGIT;
      } else {
        singleChar = '_';
        state = Letter.PUNCTUATION;
      }

      switch (state) {
        case PUNCTUATION:
          outputBuilder.append(singleChar);
          break;
        case UPPER:
          if (Letter.LOWER.equals(priorState)) {
            outputBuilder.append('_');
          }
          outputBuilder.append(singleChar);
          break;
        case LOWER:
          if (Letter.UPPER.equals(priorState)) {
            outputBuilder.append('_');
          }
          outputBuilder.append(singleChar);
          break;
        case DIGIT:
          outputBuilder.append(singleChar);
          break;
        default:
          throw new UnknownSwitchCaseException(state.name());
      }
    }

    return outputBuilder;
  }

  private static class PrometheusKey {

    private final Tag[] tags;
    private final String meterName;
    private final String quantityName;

    public PrometheusKey (String meterName, Tag[] tags, String quantityName) {

      this.meterName = meterName;
      this.tags = tags;
      this.quantityName = quantityName;
    }

    public String getMeterName () {

      return meterName;
    }

    public Tag[] getTags () {

      return tags;
    }

    public String getQuantityName () {

      return quantityName;
    }

    @Override
    public int hashCode () {

      return (((meterName.hashCode() * 31) + quantityName.hashCode()) * 31) + ((tags == null) ? 0 : Arrays.hashCode(tags));
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof PrometheusKey) && ((PrometheusKey)obj).getMeterName().equals(meterName) && ((PrometheusKey)obj).getQuantityName().equals(quantityName) && Arrays.equals(((PrometheusKey)obj).getTags(), tags);
    }
  }
}
