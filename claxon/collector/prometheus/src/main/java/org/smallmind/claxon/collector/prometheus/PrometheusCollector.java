/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.claxon.collector.prometheus;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.claxon.registry.PullCollector;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class PrometheusCollector extends PullCollector<String> {

  private enum Letter {UPPER, LOWER, DIGIT, PUNCTUATION, UNKNOWN}

  private ConcurrentHashMap<TraceKey, Trace> readMap = new ConcurrentHashMap<>();
  private volatile ConcurrentHashMap<TraceKey, Trace> writeMap = new ConcurrentHashMap<>();

  /*
  # HELP metric_name Description of the metric
  # TYPE metric_name type
  # Comment that's not parsed by prometheus
  http_requests_total{method="post",code="400"}  3   1395066363000
  */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    long nowInMilliseconds = System.currentTimeMillis();

    for (Quantity quantity : quantities) {

      writeMap.put(new TraceKey(meterName, tags, quantity.getName()), new Trace(quantity.getValue(), nowInMilliseconds));
    }
  }

  @Override
  public synchronized String emit () {

    StringBuilder outputBuilder = new StringBuilder();
    ConcurrentHashMap<TraceKey, Trace> tempMap;

    tempMap = readMap;
    readMap = writeMap;
    writeMap = tempMap;

    for (Map.Entry<TraceKey, Trace> traceEntry : readMap.entrySet()) {
      format(outputBuilder, traceEntry.getKey()).append(' ').append(traceEntry.getValue().getValue()).append(' ').append(traceEntry.getValue().getTimestamp()).append('\n');
    }

    outputBuilder.append("# EOF\n");
    readMap.clear();

    return outputBuilder.toString();
  }

  private StringBuilder format (StringBuilder outputBuilder, TraceKey traceKey) {

    mangle(outputBuilder, traceKey.getMeterName(), false);

    if ((traceKey.getTags() != null) && (traceKey.getTags().length > 0)) {

      boolean first = true;

      outputBuilder.append('{');
      for (Tag tag : traceKey.getTags()) {
        if (!first) {
          outputBuilder.append(',');
        }

        mangle(outputBuilder, tag.getKey(), true).append("=\"").append(tag.getValue()).append('"');
        first = false;
      }
      outputBuilder.append("}");
    }

    outputBuilder.append('_');
    mangle(outputBuilder, traceKey.getQuantityName(), false);

    return outputBuilder;
  }

  // Being Golang, Prometheus can't handle unicode strings like most frameworks, but only a very simple set of characters.
  private StringBuilder mangle (StringBuilder outputBuilder, String original, boolean label) {

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
        if (singleChar == '.') {
          singleChar = label ? '_' : ':';
        } else {
          singleChar = '_';
        }
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

  private static class TraceKey {

    private final Tag[] tags;
    private final String meterName;
    private final String quantityName;

    public TraceKey (String meterName, Tag[] tags, String quantityName) {

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

      return (obj instanceof TraceKey) && ((TraceKey)obj).getMeterName().equals(meterName) && ((TraceKey)obj).getQuantityName().equals(quantityName) && Arrays.equals(((TraceKey)obj).getTags(), tags);
    }
  }

  private static class Trace {

    private final double value;
    private final long timestamp;

    public Trace (double value, long timestamp) {

      this.value = value;
      this.timestamp = timestamp;
    }

    public double getValue () {

      return value;
    }

    public long getTimestamp () {

      return timestamp;
    }
  }
}
