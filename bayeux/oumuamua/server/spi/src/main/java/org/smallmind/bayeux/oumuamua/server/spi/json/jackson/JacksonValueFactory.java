/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class JacksonValueFactory implements ValueFactory<JacksonValue<?>> {

  @Override
  public JacksonValue<?> value (Object object) {

    return JacksonValueUtility.to(JsonCodec.writeAsJsonNode(object), this);
  }

  @Override
  public JacksonObjectValue objectValue () {

    return new JacksonObjectValue(JsonNodeFactory.instance.objectNode(), this);
  }

  @Override
  public JacksonArrayValue arrayValue () {

    return new JacksonArrayValue(JsonNodeFactory.instance.arrayNode(), this);
  }

  @Override
  public JacksonStringValue textValue (String text) {

    return new JacksonStringValue(JsonNodeFactory.instance.textNode(text), this);
  }

  @Override
  public JacksonNumberValue numberValue (int i) {

    return new JacksonNumberValue(JsonNodeFactory.instance.numberNode(i), this);
  }

  @Override
  public JacksonNumberValue numberValue (long l) {

    return new JacksonNumberValue(JsonNodeFactory.instance.numberNode(l), this);
  }

  @Override
  public JacksonNumberValue numberValue (double d) {

    return new JacksonNumberValue(JsonNodeFactory.instance.numberNode(d), this);
  }

  @Override
  public JacksonBooleanValue booleanValue (boolean bool) {

    return new JacksonBooleanValue(JsonNodeFactory.instance.booleanNode(bool), this);
  }

  @Override
  public JacksonNullValue nullValue () {

    return new JacksonNullValue(JsonNodeFactory.instance.nullNode(), this);
  }
}
