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
package org.smallmind.claxon.emitter.prometheus;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.claxon.registry.PullEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Pull emitter that buffers Claxon metrics and renders them on demand in the Prometheus
 * text exposition format.
 *
 * <p>Metrics are stored in a pair of {@link ConcurrentHashMap} buffers. Calls to
 * {@link #record(String, Tag[], Quantity[])} write to a volatile write buffer. Calls to
 * {@link #emit()} atomically swap read and write buffers, render all entries from the read
 * buffer, clear it, and return the resulting text. This double-buffering strategy allows
 * writes to proceed concurrently with the render cycle, though the swap and render themselves
 * are synchronised.
 *
 * <p>Metric and label names are normalised to Prometheus-compatible identifiers by
 * {@link #mangle(StringBuilder, String)}, which converts camelCase to snake_case and replaces
 * unsupported characters with underscores. Double values are formatted as Prometheus literals
 * ({@code +Inf}, {@code -Inf}, {@code NaN}, {@code 0}, or the standard decimal
 * representation).
 */
public class PrometheusEmitter extends PullEmitter<String> {

  /**
   * Character category used to track transitions between letter types during name mangling.
   */
  private enum Letter {
    /**
     * An uppercase ASCII letter ({@code A}–{@code Z}).
     */
    UPPER,
    /**
     * A lowercase ASCII letter ({@code a}–{@code z}).
     */
    LOWER,
    /**
     * An ASCII decimal digit ({@code 0}–{@code 9}).
     */
    DIGIT,
    /**
     * Any character that was replaced with an underscore.
     */
    PUNCTUATION,
    /**
     * Initial sentinel state before any character has been processed.
     */
    UNKNOWN
  }

  /**
   * The buffer from which the current {@link #emit()} call reads and renders metrics.
   * Swapped with {@link #writeMap} on each emission cycle.
   */
  private ConcurrentHashMap<PrometheusKey, Double> readMap = new ConcurrentHashMap<>();

  /**
   * The buffer into which {@link #record(String, Tag[], Quantity[])} writes the latest metric
   * values. Declared {@code volatile} so that the reference swap in {@link #emit()} is
   * visible to all threads.
   */
  private volatile ConcurrentHashMap<PrometheusKey, Double> writeMap = new ConcurrentHashMap<>();

  /*
  # HELP metric_name Description of the metric
  # TYPE metric_name type
  # Comment that's not parsed by prometheus
  http_requests_total{method="post",code="400"}  3   1395066363000
  */

  /**
   * Stores the latest value for each meter/tag/quantity combination in the write buffer,
   * overwriting any previously recorded value for the same key.
   *
   * @param meterName  the base name of the meter
   * @param tags       the tags that further identify this metric series; may be {@code null}
   *                   or empty
   * @param quantities the current measured values; must not be {@code null}
   */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

    for (Quantity quantity : quantities) {
      writeMap.put(new PrometheusKey(meterName, tags, quantity.getName()), quantity.getValue());
    }
  }

  /**
   * Atomically swaps the read and write buffers, then renders all buffered metrics as a
   * Prometheus text exposition string and clears the read buffer.
   *
   * <p>The returned string contains one sample line per stored metric in the form
   * {@code name:quantity{label="value"} numericValue} followed by a trailing
   * {@code # EOF} line. Metric and label names are normalised by
   * {@link #mangle(StringBuilder, String)}.
   *
   * @return a {@link String} containing all buffered metrics in Prometheus text format,
   * terminated by {@code # EOF\n}
   */
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

  /**
   * Converts a {@code double} value to its Prometheus text representation.
   *
   * <p>Special cases: {@link Double#POSITIVE_INFINITY} maps to {@code +Inf},
   * {@link Double#NEGATIVE_INFINITY} maps to {@code -Inf}, {@link Double#NaN} maps to
   * {@code NaN}, and {@code 0.0} maps to {@code "0"}. All other values use
   * {@link Double#toString(double)}.
   *
   * @param value the value to format
   * @return the Prometheus-compatible string representation of the value
   */
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

  /**
   * Appends the Prometheus metric identifier for the given key to {@code outputBuilder}.
   *
   * <p>The identifier is composed of the mangled meter name, a colon separator, the mangled
   * quantity name, and an optional label set enclosed in braces. Labels are formatted as
   * {@code key="value"} pairs separated by commas, with keys mangled to Prometheus format.
   *
   * @param outputBuilder the {@link StringBuilder} to append to
   * @param prometheusKey the key holding meter name, quantity name, and tags
   * @return the same {@code outputBuilder} to allow call chaining
   */
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

  /**
   * Normalises an arbitrary Java identifier or metric name to a Prometheus-compatible label or
   * metric name by converting camelCase boundaries to underscores and replacing any character
   * that is not an ASCII letter or digit with an underscore.
   *
   * <p>Prometheus metric and label names must match {@code [a-zA-Z_:][a-zA-Z0-9_:]*}. This
   * method enforces a simplified subset: it prepends an underscore when the first character is
   * a digit, inserts an underscore at lower-to-upper transitions (lowercasing the original
   * uppercase character in the process), and substitutes any other non-alphanumeric character
   * with an underscore.
   *
   * @param outputBuilder the {@link StringBuilder} to append the mangled name to
   * @param original      the original name to normalise
   * @return the same {@code outputBuilder} to allow call chaining
   * @throws UnknownSwitchCaseException if an unhandled {@link Letter} state is encountered
   */
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
          outputBuilder.append(Character.toLowerCase(singleChar));
          break;
        case LOWER, DIGIT:
          outputBuilder.append(singleChar);
          break;
        default:
          throw new UnknownSwitchCaseException(state.name());
      }
    }

    return outputBuilder;
  }

  /**
   * Composite map key that uniquely identifies a single Prometheus sample by its meter name,
   * quantity name, and tag set.
   *
   * <p>Equality and hashing account for all three components, using
   * {@link Arrays#hashCode(Object[])} and {@link Arrays#equals(Object[], Object[])} for the
   * tag array so that tag order is significant.
   */
  private static class PrometheusKey {

    /**
     * The tags associated with this metric series; may be {@code null}.
     */
    private final Tag[] tags;

    /**
     * The meter name component of this key.
     */
    private final String meterName;

    /**
     * The quantity name component of this key.
     */
    private final String quantityName;

    /**
     * Constructs a composite key from the given meter name, tags, and quantity name.
     *
     * @param meterName    the name of the meter
     * @param tags         the tags identifying this series; may be {@code null}
     * @param quantityName the name of the specific quantity within the meter
     */
    public PrometheusKey (String meterName, Tag[] tags, String quantityName) {

      this.meterName = meterName;
      this.tags = tags;
      this.quantityName = quantityName;
    }

    /**
     * Returns the meter name component of this key.
     *
     * @return the meter name
     */
    public String getMeterName () {

      return meterName;
    }

    /**
     * Returns the tags component of this key.
     *
     * @return the tag array, which may be {@code null}
     */
    public Tag[] getTags () {

      return tags;
    }

    /**
     * Returns the quantity name component of this key.
     *
     * @return the quantity name
     */
    public String getQuantityName () {

      return quantityName;
    }

    /**
     * Computes a hash code from the meter name, quantity name, and tag array.
     *
     * @return the hash code
     */
    @Override
    public int hashCode () {

      return (((meterName.hashCode() * 31) + quantityName.hashCode()) * 31) + ((tags == null) ? 0 : Arrays.hashCode(tags));
    }

    /**
     * Returns {@code true} if {@code obj} is a {@link PrometheusKey} with equal meter name,
     * quantity name, and tag array.
     *
     * @param obj the object to compare with
     * @return {@code true} if all three components are equal
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof PrometheusKey) && ((PrometheusKey)obj).getMeterName().equals(meterName) && ((PrometheusKey)obj).getQuantityName().equals(quantityName) && Arrays.equals(((PrometheusKey)obj).getTags(), tags);
    }
  }
}
