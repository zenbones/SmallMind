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
package org.smallmind.nutsnbolts.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes rows of comma-separated values, handling quoting and escaping.
 */
public class CSVWriter implements AutoCloseable {

  private static final char[] ESCAPED_CHARS = {'"', ',', '\n', '\r', '\f'};

  private final OutputStream outputStream;
  private final int lineLength;

  /**
   * Creates a writer that immediately emits the header row.
   *
   * @param outputStream destination stream
   * @param headers      header values (establishing the expected field count)
   * @throws IOException       if writing fails
   * @throws CSVParseException if header count is invalid
   */
  public CSVWriter (OutputStream outputStream, String... headers)
    throws IOException, CSVParseException {

    this.outputStream = outputStream;

    lineLength = headers.length;
    write(headers);
  }

  /**
   * Creates a writer expecting a fixed number of fields per row.
   *
   * @param outputStream destination stream
   * @param lineLength   required number of fields
   */
  public CSVWriter (OutputStream outputStream, int lineLength) {

    this.outputStream = outputStream;
    this.lineLength = lineLength;
  }

  /**
   * Writes a single CSV record.
   *
   * @param fields field values to write
   * @throws IOException       if writing fails
   * @throws CSVParseException if the number of fields does not match {@code lineLength}
   */
  public void write (String... fields)
    throws IOException, CSVParseException {

    boolean init = false;

    if (fields.length != lineLength) {
      throw new CSVParseException("Line must contain the set number of fields(%d)", lineLength);
    }

    for (String field : fields) {
      if (init) {
        outputStream.write(',');
      }

      if (mustBeQuoted(field)) {
        outputStream.write('"');
        outputStream.write(doubleAllQuotes(field).getBytes(StandardCharsets.UTF_8));
        outputStream.write('"');
      } else {
        outputStream.write(doubleAllQuotes(field).getBytes(StandardCharsets.UTF_8));
      }

      init = true;
    }

    outputStream.write('\n');
  }

  /**
   * Doubles any embedded quotes within a field.
   *
   * @param field field to escape
   * @return field with quotes escaped
   */
  private String doubleAllQuotes (String field) {

    if (field.indexOf("\"") >= 0) {
      return field.replaceAll("\"", "\"\"");
    }

    return field;
  }

  /**
   * Determines if a field must be surrounded by quotes when written.
   *
   * @param field field to examine
   * @return {@code true} if the field contains a special character
   */
  private boolean mustBeQuoted (String field) {

    for (char escapeChar : ESCAPED_CHARS) {
      if (field.indexOf(escapeChar) >= 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Closes the underlying stream.
   */
  public void close ()
    throws IOException {

    outputStream.close();
  }
}
