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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Streaming CSV parser that delegates each parsed row to a {@link CSVLineHandler}.
 */
public class CSVParser {

  private CSVLineHandler lineHandler;
  private boolean skipHeader = false;
  private boolean trimFields = false;

  /**
   * @return handler invoked for each parsed line
   */
  public synchronized CSVLineHandler getLineHandler () {

    return lineHandler;
  }

  /**
   * Sets the handler invoked for each parsed line.
   *
   * @param lineHandler consumer for parsed rows
   */
  public synchronized void setLineHandler (CSVLineHandler lineHandler) {

    this.lineHandler = lineHandler;
  }

  /**
   * @return {@code true} when the first line should be treated as a header and skipped
   */
  public synchronized boolean isSkipHeader () {

    return skipHeader;
  }

  /**
   * Indicates whether to skip the first line of the input.
   *
   * @param skipHeader {@code true} to ignore the header row
   */
  public synchronized void setSkipHeader (boolean skipHeader) {

    this.skipHeader = skipHeader;
  }

  /**
   * @return {@code true} when parsed fields should be trimmed
   */
  public synchronized boolean isTrimFields () {

    return trimFields;
  }

  /**
   * Configures whether surrounding whitespace is removed from fields.
   *
   * @param trimFields {@code true} to trim field values
   */
  public synchronized void setTrimFields (boolean trimFields) {

    this.trimFields = trimFields;
  }

  /**
   * Parses CSV from an input stream using the current settings.
   *
   * @param inputStream stream of CSV data
   * @throws IOException       if reading fails
   * @throws CSVParseException if parsing fails
   */
  public synchronized void parse (InputStream inputStream)
    throws IOException, CSVParseException {

    parse(new InputStreamReader(inputStream));
  }

  /**
   * Parses CSV from a reader using the current settings.
   *
   * @param reader source of CSV data
   * @throws IOException       if reading fails
   * @throws CSVParseException if parsing fails
   */
  public synchronized void parse (Reader reader)
    throws IOException, CSVParseException {

    CSVReader csvReader;
    String[] fields;

    csvReader = new CSVReader(reader, skipHeader);
    csvReader.setTrimFields(trimFields);

    lineHandler.startDocument();

    while ((fields = csvReader.readLine()) != null) {
      lineHandler.handleFields(fields);
    }

    lineHandler.endDocument();
  }
}
