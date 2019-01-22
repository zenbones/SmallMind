/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

public class CSVReader implements AutoCloseable {

  private static enum State {

    UNQOUTED, QUOTED
  }

  private BufferedReader reader;
  private StringBuilder fieldBuilder;
  private State state;
  private String[] headers;
  private boolean trimFields;

  public CSVReader (InputStream stream)
    throws IOException, CSVParseException {

    this(new InputStreamReader(stream), false);
  }

  public CSVReader (InputStream stream, boolean useHeaders)
    throws IOException, CSVParseException {

    this(new InputStreamReader(stream), useHeaders);
  }

  public CSVReader (Reader reader)
    throws IOException, CSVParseException {

    this(reader, false);
  }

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

  public synchronized CSVReader setTrimFields (boolean trimFields) {

    this.trimFields = trimFields;

    return this;
  }

  public synchronized String[] getHeaders () {

    return headers;
  }

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

  public synchronized String[] readLine ()
    throws IOException, CSVParseException {

    return readLine(trimFields);
  }

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
                case '\"':
                  if (fieldBuilder.length() > 0) {
                    throw new CSVParseException("The first character in a quoted field must be '\"'");
                  }
                  else {
                    state = State.QUOTED;
                  }
                  break;
                default:
                  fieldBuilder.append(singleLine.charAt(count));
              }
              break;
            case QUOTED:
              switch (singleLine.charAt(count)) {
                case '\"':
                  if ((count < (singleLine.length() - 1)) && (!((singleLine.charAt(count + 1) == '\"') || (singleLine.charAt(count + 1) == ',')))) {
                    throw new CSVParseException("The last character in a quoted field must be '\"'");
                  }
                  else if ((count == (singleLine.length() - 1)) || (singleLine.charAt(count + 1) == ',')) {
                    state = State.UNQOUTED;
                  }
                  else {
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
        }
        else {
          appendField(fieldList, trimFields);

          fields = new String[fieldList.size()];
          fieldList.toArray(fields);

          return fields;
        }
      }
      else {
        if (state.equals(State.QUOTED)) {
          throw new CSVParseException("Reached the end of the stream with an open quoted field");
        }
        else {
          return null;
        }
      }
    }
  }

  private void appendField (LinkedList<String> fieldList, boolean trimFields) {

    fieldList.add((trimFields) ? fieldBuilder.toString().trim() : fieldBuilder.toString());
    fieldBuilder.setLength(0);
  }

  public synchronized void close ()
    throws IOException {

    reader.close();
  }
}
