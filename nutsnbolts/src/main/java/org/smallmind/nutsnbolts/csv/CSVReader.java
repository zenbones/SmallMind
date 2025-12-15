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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;

/**
 * Reads comma-separated values from a stream, handling quoted fields and optional headers.
 */
public class CSVReader implements AutoCloseable {

  private enum State {

    UNQOUTED, QUOTED
  }

  private final BufferedReader reader;
  private final StringBuilder fieldBuilder;
  private State state;
  private String[] headers;
  private boolean trimFields;

  /**
   * Creates a reader over an input stream without headers.
   *
   * @param stream CSV byte stream
   * @throws IOException       if the stream cannot be read
   * @throws CSVParseException if parsing fails
   */
  public CSVReader (InputStream stream)
    throws IOException, CSVParseException {

    this(new InputStreamReader(stream), false);
  }

  /**
   * Creates a reader over an input stream, optionally using the first line as headers.
   *
   * @param stream     CSV byte stream
   * @param useHeaders whether to read the first line as headers
   * @throws IOException       if the stream cannot be read
   * @throws CSVParseException if parsing fails
   */
  public CSVReader (InputStream stream, boolean useHeaders)
    throws IOException, CSVParseException {

    this(new InputStreamReader(stream), useHeaders);
  }

  /**
   * Creates a reader over a character reader without headers.
   *
   * @param reader CSV character stream
   * @throws IOException       if the stream cannot be read
   * @throws CSVParseException if parsing fails
   */
  public CSVReader (Reader reader)
    throws IOException, CSVParseException {

    this(reader, false);
  }

  /**
   * Creates a reader over a character reader, optionally using the first line as headers.
   *
   * @param reader     CSV character stream
   * @param useHeaders whether to read the first line as headers
   * @throws IOException       if the stream cannot be read
   * @throws CSVParseException if parsing fails
   */
  public CSVReader (Reader reader, boolean useHeaders)
    throws IOException, CSVParseException {

    this.reader = new BufferedReader(reader);

    state = State.UNQOUTED;
    trimFields = false;
    fieldBuilder = new StringBuilder();

    if (useHeaders) {
      headers = readLine(true);
    }
  }

  /**
   * Controls whether returned field values are trimmed.
   *
   * @param trimFields {@code true} to trim whitespace
   * @return this reader for chaining
   */
  public synchronized CSVReader setTrimFields (boolean trimFields) {

    this.trimFields = trimFields;

    return this;
  }

  /**
   * @return header fields or {@code null} if headers were not requested
   */
  public synchronized String[] getHeaders () {

    return headers;
  }

  /**
   * Retrieves a field by header name from a parsed row.
   *
   * @param header header name to look up
   * @param fields parsed row fields
   * @return field value or {@code null} if the header is not present
   */
  public synchronized String getField (String header, String[] fields) {

    if (headers == null) {
      throw new IllegalStateException("No headers are available");
    }

    for (int count = 0; count < headers.length; count++) {
      if (headers[count].equals(header)) {
        return fields[count];
      }
    }

    return null;
  }

  /**
   * Reads the next record from the stream using the configured trim setting.
   *
   * @return array of fields or {@code null} at end of stream
   * @throws IOException       if reading fails
   * @throws CSVParseException if the CSV is malformed
   */
  public synchronized String[] readLine ()
    throws IOException, CSVParseException {

    return readLine(trimFields);
  }

  /**
   * Internal line reader that respects the supplied trimming preference.
   *
   * @param trimFields whether to strip whitespace from fields
   * @return parsed fields or {@code null} at end of stream
   * @throws IOException       if reading fails
   * @throws CSVParseException if CSV structure is invalid
   */
  private synchronized String[] readLine (boolean trimFields)
    throws IOException, CSVParseException {

    LinkedList<String> fieldList;
    String[] fields;
    String singleLine;

    fieldList = new LinkedList<>();
    while (true) {
      if ((singleLine = reader.readLine()) != null) {
        for (int count = 0; count < singleLine.length(); count++) {
          switch (state) {
            case UNQOUTED:
              switch (singleLine.charAt(count)) {
                case ',':
                  appendField(fieldList, trimFields);
                  break;
                case '"':
                  if (fieldBuilder.length() > 0) {
                    throw new CSVParseException("The first character in a quoted field must be '\"'");
                  } else {
                    state = State.QUOTED;
                  }
                  break;
                default:
                  fieldBuilder.append(singleLine.charAt(count));
              }
              break;
            case QUOTED:
              switch (singleLine.charAt(count)) {
                case '"':
                  if ((count < (singleLine.length() - 1)) && (!((singleLine.charAt(count + 1) == '"') || (singleLine.charAt(count + 1) == ',')))) {
                    throw new CSVParseException("The last character in a quoted field must be '\"'");
                  } else if ((count == (singleLine.length() - 1)) || (singleLine.charAt(count + 1) == ',')) {
                    state = State.UNQOUTED;
                  } else {
                    fieldBuilder.append(singleLine.charAt(count));
                    count++;
                  }
                  break;
                default:
                  fieldBuilder.append(singleLine.charAt(count));
              }
          }
        }

        if (state.equals(State.QUOTED)) {
          fieldBuilder.append(System.getProperty("line.separator"));
        } else {
          appendField(fieldList, trimFields);

          fields = new String[fieldList.size()];
          fieldList.toArray(fields);

          return fields;
        }
      } else {
        if (state.equals(State.QUOTED)) {
          throw new CSVParseException("Reached the end of the stream with an open quoted field");
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Adds the current field buffer to the accumulating list, trimming if configured.
   */
  private void appendField (LinkedList<String> fieldList, boolean trimFields) {

    fieldList.add((trimFields) ? fieldBuilder.toString().strip() : fieldBuilder.toString());
    fieldBuilder.setLength(0);
  }

  public synchronized void close ()
    throws IOException {

    reader.close();
  }
}
