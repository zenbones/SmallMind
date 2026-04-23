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
package org.smallmind.scribe.pen;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatter that converts log records to strings according to a configurable pattern syntax.
 * A pattern is composed of literal text and conversion tokens of the form
 * {@code [{header]%[<+|->width][.precision][!(+|-)prefix!]conversion[footer}]}; available
 * conversion characters are: {@code d} (date), {@code t} (epoch millis), {@code n} (logger name),
 * {@code l} (level), {@code m} (message), {@code T} (thread name), {@code C} (class),
 * {@code M} (method), {@code N} (native flag), {@code L} (line number), {@code F} (file name),
 * {@code s} (stack trace), and {@code p} (parameters). The default pattern is
 * {@code "%d %n %+5l [%T] - %m"}.
 */
public class PatternFormatter implements Formatter {

  /*
  Operation of this parser is similar to that of String formatting Flags. Each Flag has the general form
  of ({[^%]+)?%((+|-)?(\d+))?(.\d*)?(!(+|-)[^!]*!)?([dtnlmTCMNLFsp])([^}]+})? or
  [{header]%[<+|->width][.precision][!(+|-)prefix!]conversion[footer}].

  [{<header text>] is used to insert starting text (only if the field value exists and will be output).
  [<footer text>}] is used to insert ending text (only if the field value exists and will be output).

  [<+|->width] is used to set the maximum field length, where the optional '+' or '-' is used to denote right or left
  padded formatting where the field length is less than the width specifier. If absent, then no padding will be used.

  [.precision] is used in dot notated fields (logger name and context class) to specify a maximum number of segments
  to display, starting from the right. For example, given a logger name of 'com.mydomain.myproject.MyClass' and a
  format flag of %.2n, the conversion would print 'myproject.MyClass'. The precision specifier is used with multi-line
  fields (currently just Parameters), to specify the maximum number of lines displayed (as a multi-line list). The
  precision specifier will be ignored on all other field types.

  [!(+|-)<prefix text>!] is used to specify a line separator and any line prefix text to insert before each line of a
  multi-line field e.g. Parameters. The '+' or '-' must be present, and sets whether the first line should be prefixed
  with the text, '+' for true and '-' for false. For instance, the flag '!-,\n! would tell the formatter to insert a
  comma followed by a line-break before each line of a multi-line field, excluding the first, which will present a
  comma separated list. If you desire text after the last line, use a footer flag. The default is equivalent to
  !+\n\t!, or a new-line followed by a tab starting each output line, including the first.

  Available conversions are...

  d - The date stamp of the log entry (yyyy-MM-dd'T'HH:mm:ss.SSSZ)
  t - The time stamp of the entry in milliseconds
  n - The logger name
  l - The logger level
  m - The log message
  T - The name of the thread in which the logging occurred (if available)
  C - The class from which the log event was issued (if available)
  M - The method in which the log event was issued (if available)
  N - Whether the method which issued the log event was native code or not [true or false] (if available)
  L - The line number in the class file from which the log event was issued (if available)
  F - The file name of the class file from which the log event was issued (if available)
  s - The stack trace associated with the log event (if present)
  p - The parameters associated with the log event (if present)

  s and p (stack trace and parameters) are multi-line fields, and are
  unaffected by the width or padding specifiers.

  The sequence %% outputs a single '%'.
  The character '\n' will be replaced by the machine specific line separator.
  */

  private static final Pattern CONVERSION_PATTERN = Pattern.compile("%%|(\\{([^{}]+))?%(([+\\-])?(\\d+))?(\\.(\\d+))?(!([+\\-])([^!]*)!)?([dtnlmTCMNLFsp])(([^{%]+)})?");
  private static final StaticPatternRule DOUBLE_PERCENT_RULE = new StaticPatternRule("%");
  private PatternRule[] patternRules;
  private Timestamp timestamp;

  /**
   * Constructs a formatter using the default {@link DateFormatTimestamp} and the default pattern
   * {@code "%d %n %+5l [%T] - %m"}.
   */
  public PatternFormatter () {

    this(DateFormatTimestamp.getDefaultInstance(), "%d %n %+5l [%T] - %m");
  }

  /**
   * Constructs a formatter using the default {@link DateFormatTimestamp} and the supplied pattern.
   *
   * @param format the pattern format string to parse and apply; must not be {@code null}
   */
  public PatternFormatter (String format) {

    this(DateFormatTimestamp.getDefaultInstance(), format);
  }

  /**
   * Constructs a formatter with an explicit timestamp provider and pattern. If {@code format} is
   * {@code null}, no pattern rules are installed and every record formats to an empty line.
   *
   * @param timestamp the timestamp provider used for {@code %d} conversions
   * @param format    the pattern format string to parse, or {@code null} to install no rules
   */
  public PatternFormatter (Timestamp timestamp, String format) {

    this.timestamp = timestamp;

    if (format != null) {
      setFormat(format);
    }
  }

  /**
   * Returns the timestamp provider used to render {@code %d} (date) conversion tokens.
   *
   * @return the active timestamp provider
   */
  public Timestamp getTimestamp () {

    return timestamp;
  }

  /**
   * Sets the timestamp provider used to render {@code %d} (date) conversion tokens.
   *
   * @param timestamp the new timestamp provider; must not be {@code null}
   */
  public void setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * Parses the given pattern format string and replaces the current set of pattern rules. Literal
   * text between conversion tokens becomes {@link StaticPatternRule} instances; each matched
   * conversion token becomes a {@link ConversionPatternRule}; and the {@code %%} escape becomes a
   * static rule that emits a single {@code %}.
   *
   * @param format the pattern format string to parse; must not be {@code null}
   */
  public void setFormat (String format) {

    Matcher conversionMatcher = CONVERSION_PATTERN.matcher(format);
    LinkedList<PatternRule> ruleList;
    int index = 0;

    ruleList = new LinkedList<>();
    while (conversionMatcher.find(index)) {
      if (index < conversionMatcher.start()) {
        ruleList.add(new StaticPatternRule(format.substring(index, conversionMatcher.start())));
      }

      if (conversionMatcher.group().equals("%%")) {
        ruleList.add(DOUBLE_PERCENT_RULE);
      } else {
        ruleList.add(new ConversionPatternRule(conversionMatcher.group(2), conversionMatcher.group(4), conversionMatcher.group(5), conversionMatcher.group(7), conversionMatcher.group(9), conversionMatcher.group(10), conversionMatcher.group(11), conversionMatcher.group(13)));
      }

      index = conversionMatcher.end();
    }

    if (index < format.length()) {
      ruleList.add(new StaticPatternRule(format.substring(index)));
    }

    patternRules = new PatternRule[ruleList.size()];
    ruleList.toArray(patternRules);
  }

  /**
   * Applies all pattern rules to the record and returns the resulting string, appending the
   * platform line separator at the end. Rules whose {@code convert()} method returns {@code null}
   * are omitted, including their header and footer.
   *
   * @param record the log record to format
   * @return the formatted log line, terminated by the platform-specific line separator
   */
  public String format (Record<?> record) {

    StringBuilder formatBuilder = new StringBuilder();
    String conversion;
    String header;
    String footer;

    for (PatternRule patternRule : patternRules) {
      if ((conversion = patternRule.convert(record, timestamp)) != null) {
        if ((header = patternRule.getHeader()) != null) {
          formatBuilder.append(header);
        }

        formatBuilder.append(conversion);

        if ((footer = patternRule.getFooter()) != null) {
          formatBuilder.append(footer);
        }
      }
    }

    formatBuilder.append(System.getProperty("line.separator"));

    return formatBuilder.toString();
  }
}
