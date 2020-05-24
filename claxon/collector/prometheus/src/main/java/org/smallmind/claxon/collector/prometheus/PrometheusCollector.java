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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.claxon.registry.PullCollector;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class PrometheusCollector extends PullCollector<String> {

  private enum Letter {UPPER, LOWER, DIGIT, PUNCTUATION, UNKNOWN}

  private final ConcurrentLinkedQueue<String> rowQueue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger rowCount = new AtomicInteger(0);
  private final int maxRows;
  /*
  # HELP metric_name Description of the metric
  # TYPE metric_name type
  # Comment that's not parsed by prometheus
  http_requests_total{method="post",code="400"}  3   1395066363000
  */

  public PrometheusCollector () {

    this(Integer.MAX_VALUE - 1);
  }

  public PrometheusCollector (int maxRows) {

    if ((maxRows < 1) || (maxRows == Integer.MAX_VALUE)) {
      this.maxRows = Integer.MAX_VALUE - 1;
    } else {
      this.maxRows = maxRows;
    }
  }

  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    StringBuilder identityBuilder = mangle(meterName, new StringBuilder(), false);
    StringBuilder labelBuilder = null;
    long nowInMilliseconds = System.currentTimeMillis();

    if ((tags != null) && (tags.length > 0)) {

      labelBuilder = new StringBuilder();
      boolean first = true;

      labelBuilder.append('{');
      for (Tag tag : tags) {
        if (!first) {
          labelBuilder.append(',');
        }
        mangle(tag.getKey(), labelBuilder, true).append("=\"").append(tag.getValue()).append('"');
        first = false;
      }
      labelBuilder.append("}");
    }

    for (Quantity quantity : quantities) {

      StringBuilder rowBuilder = new StringBuilder(identityBuilder);

      mangle(quantity.getName(), rowBuilder.append('_'), false);
      if (labelBuilder != null) {
        rowBuilder.append(labelBuilder);
      }
      rowBuilder.append(' ').append(quantity.getValue()).append(' ').append(nowInMilliseconds);

      if (rowCount.incrementAndGet() > maxRows) {
        if (rowQueue.poll() != null) {
          rowCount.decrementAndGet();
        }
      }
      rowQueue.add(rowBuilder.toString());
    }
  }

  @Override
  public String emit () {

    if (rowQueue.isEmpty()) {

      return "\n";
    } else {

      StringBuilder outputBuilder = new StringBuilder();
      String row;

      while ((row = rowQueue.poll()) != null) {
        rowCount.decrementAndGet();
        outputBuilder.append(row).append('\n');
      }
      outputBuilder.append('\n');

      return outputBuilder.toString();
    }
  }

  // Being Golang, Prometheus can't handle unicode strings like most frameworks, but only a very simple set of characters.
  private StringBuilder mangle (String original, StringBuilder mangledBuilder, boolean label) {

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
          mangledBuilder.append('_');
        }
        state = Letter.DIGIT;
      } else {
        if (singleChar == '.') {
          singleChar = label ? '_' : '-';
        } else if (singleChar != '-') {
          singleChar = '_';
        }
        state = Letter.PUNCTUATION;
      }

      switch (state) {
        case PUNCTUATION:
          mangledBuilder.append(singleChar);
          break;
        case UPPER:
          if (Letter.LOWER.equals(priorState)) {
            mangledBuilder.append('_');
          }
          mangledBuilder.append(singleChar);
          break;
        case LOWER:
          if (Letter.UPPER.equals(priorState)) {
            mangledBuilder.append('_');
          }
          mangledBuilder.append(singleChar);
          break;
        case DIGIT:
          mangledBuilder.append(singleChar);
          break;
        default:
          throw new UnknownSwitchCaseException(state.name());
      }
    }

    return mangledBuilder;
  }
}
