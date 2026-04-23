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
 * Low-level CSV reader that parses quoted and unquoted fields from a character or byte stream, with optional header row consumption and per-field whitespace trimming.
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
   * Creates a reader wrapping the given input stream, treating no line as a header row.
   *
   * @param stream CSV byte stream to read
   * @throws IOException       if the underlying stream cannot be opened
   * @throws CSVParseException if the initial read fails
   */
  public CSVReader (InputStream stream)
    throws IOException, CSVParseException {

    this(new InputStreamReader(stream), false);
  }

  /**
   * Creates a reader wrapping the given input stream, optionally consuming the first line as a header row.
   *
   * @param stream     CSV byte stream to read
   * @param useHeaders {@code true} to read and retain the first line as column headers
   * @throws IOException       if the underlying stream cannot be opened
   * @throws CSVParseException if the initial read or header parsing fails
   */
  public CSVReader (InputStream stream, boolean useHeaders)
    throws IOException, CSVParseException {

    this(new InputStreamReader(stream), useHeaders);
  }

  /**
   * Creates a reader wrapping the given character reader, treating no line as a header row.
   *
   * @param reader CSV character stream to read
   * @throws IOException       if the underlying reader cannot be opened
   * @throws CSVParseException if the initial read fails
   */
  public CSVReader (Reader reader)
    throws IOException, CSVParseException {

    this(reader, false);
  }

  /**
   * Creates a reader wrapping the given character reader, optionally consuming the first line as a header row.
   *
   * @param reader     CSV character stream to read
   * @param useHeaders {@code true} to read and retain the first line as column headers
   * @throws IOException       if the underlying reader cannot be opened
   * @throws CSVParseException if the initial read or header parsing fails
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
   * Enables or disables stripping of leading and trailing whitespace from each field value returned by {@link #readLine()}.
   *
   * @param trimFields {@code true} to strip whitespace from field values
   * @return this reader, to allow fluent chaining
   */
  public synchronized CSVReader setTrimFields (boolean trimFields) {

    this.trimFields = trimFields;

    return this;
  }

  /**
   * Returns the header fields consumed from the first row, or {@code null} if this reader was not constructed with header support.
   *
   * @return array of header field values, or {@code null}
   */
  public synchronized String[] getHeaders () {

    return headers;
  }

  /**
   * Returns the value from a parsed record corresponding to the named header column.
   *
   * @param header column name to look up in the header row
   * @param fields array of field values for the current record, as returned by {@link #readLine()}
   * @return the field value at the header's position, or {@code null} if the header name is not found
   * @throws IllegalStateException if this reader was not constructed with header support
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
   * Reads and returns the next record from the underlying stream, applying the configured trim setting.
   *
   * @return array of field values for the next record, or {@code null} when the end of the stream is reached
   * @throws IOException       if a read error occurs on the underlying stream
   * @throws CSVParseException if the CSV structure is invalid, such as an unterminated quoted field
   */
  public synchronized String[] readLine ()
    throws IOException, CSVParseException {

    return readLine(trimFields);
  }

  /**
   * Reads and parses the next CSV record from the stream, honouring the supplied trimming flag and handling multi-line quoted fields.
   *
   * @param trimFields {@code true} to strip leading and trailing whitespace from each field
   * @return array of field values, or {@code null} at end of stream
   * @throws IOException       if a read error occurs on the underlying reader
   * @throws CSVParseException if the CSV structure is invalid, such as an unterminated quoted field or misplaced quote
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
   * Flushes the current field buffer into the field list, optionally trimming whitespace, and resets the buffer for the next field.
   *
   * @param fieldList  accumulating list of parsed fields for the current record
   * @param trimFields {@code true} to strip leading and trailing whitespace from the buffered value
   */
  private void appendField (LinkedList<String> fieldList, boolean trimFields) {

    fieldList.add((trimFields) ? fieldBuilder.toString().strip() : fieldBuilder.toString());
    fieldBuilder.setLength(0);
  }

  /**
   * Closes the underlying buffered reader and releases any associated I/O resources.
   *
   * @throws IOException if closing the reader fails
   */
  public synchronized void close ()
    throws IOException {

    reader.close();
  }
}
