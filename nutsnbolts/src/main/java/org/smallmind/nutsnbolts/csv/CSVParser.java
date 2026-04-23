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
 * Configurable, event-driven CSV parser that reads a stream line-by-line and delegates each parsed record to a {@link CSVLineHandler}.
 */
public class CSVParser {

  private CSVLineHandler lineHandler;
  private boolean skipHeader = false;
  private boolean trimFields = false;

  /**
   * Returns the handler currently registered to receive parsed records.
   *
   * @return the active {@link CSVLineHandler}, or {@code null} if none has been set
   */
  public synchronized CSVLineHandler getLineHandler () {

    return lineHandler;
  }

  /**
   * Sets the handler that will receive document lifecycle and per-record callbacks during parsing.
   *
   * @param lineHandler handler to invoke for each parsed record
   */
  public synchronized void setLineHandler (CSVLineHandler lineHandler) {

    this.lineHandler = lineHandler;
  }

  /**
   * Returns whether the first line of the input will be treated as a header row and skipped.
   *
   * @return {@code true} if the header row is skipped
   */
  public synchronized boolean isSkipHeader () {

    return skipHeader;
  }

  /**
   * Configures whether to skip the first line of the input as a header row.
   *
   * @param skipHeader {@code true} to consume and discard the header row before processing records
   */
  public synchronized void setSkipHeader (boolean skipHeader) {

    this.skipHeader = skipHeader;
  }

  /**
   * Returns whether leading and trailing whitespace is stripped from each field value.
   *
   * @return {@code true} if field trimming is enabled
   */
  public synchronized boolean isTrimFields () {

    return trimFields;
  }

  /**
   * Configures whether surrounding whitespace is stripped from each parsed field value.
   *
   * @param trimFields {@code true} to trim whitespace from field values
   */
  public synchronized void setTrimFields (boolean trimFields) {

    this.trimFields = trimFields;
  }

  /**
   * Parses CSV data from the given input stream, delegating each record to the registered {@link CSVLineHandler}.
   *
   * @param inputStream source of CSV bytes
   * @throws IOException       if a read error occurs on the stream
   * @throws CSVParseException if the CSV structure is invalid or the handler rejects a record
   */
  public synchronized void parse (InputStream inputStream)
    throws IOException, CSVParseException {

    parse(new InputStreamReader(inputStream));
  }

  /**
   * Parses CSV data from the given reader, delegating each record to the registered {@link CSVLineHandler}.
   *
   * @param reader source of CSV character data
   * @throws IOException       if a read error occurs
   * @throws CSVParseException if the CSV structure is invalid or the handler rejects a record
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
