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
package org.smallmind.bayeux.oumuamua.server.spi.json.orthodox;

import java.io.IOException;
import java.io.Writer;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import tools.jackson.core.io.JsonStringEncoder;

/**
 * Immutable {@link StringValue} implementation for the orthodox codec that stores a Java string and
 * encodes it as a properly escaped JSON string using the Jackson {@link JsonStringEncoder}.
 */
public class OrthodoxTextValue extends OrthodoxValue implements StringValue<OrthodoxValue> {

  private final String text;

  /**
   * Constructs a string value associated with the given factory.
   *
   * @param factory the {@link OrthodoxValueFactory} that owns this value
   * @param text    the string to wrap; may be empty but should not be {@code null}
   */
  protected OrthodoxTextValue (OrthodoxValueFactory factory, String text) {

    super(factory);

    this.text = text;
  }

  /**
   * Returns the raw wrapped string without any escaping applied.
   *
   * @return the original string as stored at construction
   */
  @Override
  public String asText () {

    return text;
  }

  /**
   * Writes the JSON string representation of the text to {@code writer}, surrounding it with
   * double-quotes and applying Jackson's JSON character escaping.
   *
   * @param writer destination for the JSON output
   * @throws IOException if writing to {@code writer} fails
   */
  @Override
  public void encode (Writer writer)
    throws IOException {

    writer.write('"');
    writer.write(JsonStringEncoder.getInstance().quoteAsCharArray(text));
    writer.write('"');
  }
}
