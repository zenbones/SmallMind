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
 * Writes records as comma-separated values to an output stream, quoting fields that contain commas, quotes, or newline characters and enforcing a fixed field count per record.
 */
public class CSVWriter implements AutoCloseable {

  private static final char[] ESCAPED_CHARS = {'"', ',', '\n', '\r', '\f'};

  private final OutputStream outputStream;
  private final int lineLength;

  /**
   * Creates a writer that immediately emits the supplied values as a header row and uses the header count as the required field count for subsequent records.
   *
   * @param outputStream destination stream to write to
   * @param headers      header field values to write as the first row
   * @throws IOException       if writing the header row fails
   * @throws CSVParseException if the header array length is inconsistent with this writer's field count
   */
  public CSVWriter (OutputStream outputStream, String... headers)
    throws IOException, CSVParseException {

    this.outputStream = outputStream;

    lineLength = headers.length;
    write(headers);
  }

  /**
   * Creates a writer that enforces the specified number of fields per record without writing a header row.
   *
   * @param outputStream destination stream to write to
   * @param lineLength   required number of fields for every record written by this writer
   */
  public CSVWriter (OutputStream outputStream, int lineLength) {

    this.outputStream = outputStream;
    this.lineLength = lineLength;
  }

  /**
   * Writes a single record to the stream, separating fields with commas, quoting fields that require it, and appending a newline.
   *
   * @param fields field values to write; must contain exactly {@code lineLength} elements
   * @throws IOException       if writing to the underlying stream fails
   * @throws CSVParseException if the number of supplied fields does not equal the writer's required field count
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
   * Returns a copy of the field with every embedded double-quote character escaped as two consecutive double-quotes.
   *
   * @param field the field value to escape
   * @return the field with all {@code "} characters replaced by {@code ""}
   */
  private String doubleAllQuotes (String field) {

    if (field.indexOf("\"") >= 0) {
      return field.replaceAll("\"", "\"\"");
    }

    return field;
  }

  /**
   * Returns {@code true} if the field contains any character that requires the value to be wrapped in double-quotes when written.
   *
   * @param field the field value to inspect
   * @return {@code true} if the field must be quoted
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
   * Closes the underlying output stream and releases any associated I/O resources.
   *
   * @throws IOException if closing the stream fails
   */
  public void close ()
    throws IOException {

    outputStream.close();
  }
}
