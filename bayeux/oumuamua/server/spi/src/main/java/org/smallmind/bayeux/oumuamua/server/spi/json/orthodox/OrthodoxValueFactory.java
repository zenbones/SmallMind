/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NullValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;

public class OrthodoxValueFactory implements ValueFactory<OrthodoxValue> {

  @Override
  public ObjectValue<OrthodoxValue> objectValue () {

    return new OrthodoxObjectValue(this);
  }

  @Override
  public ArrayValue<OrthodoxValue> arrayValue () {

    return new OrthodoxArrayValue(this);
  }

  @Override
  public StringValue<OrthodoxValue> textValue (String text) {

    return new OrthodoxTextValue(this, text);
  }

  @Override
  public NumberValue<OrthodoxValue> numberValue (int i) {

    return new OrthodoxIntegerValue(this, i);
  }

  @Override
  public NumberValue<OrthodoxValue> numberValue (long l) {

    return new OrthodoxLongValue(this, l);
  }

  @Override
  public NumberValue<OrthodoxValue> numberValue (double d) {

    return new OrthodoxDoubleValue(this, d);
  }

  @Override
  public BooleanValue<OrthodoxValue> booleanValue (boolean bool) {

    return new OrthodoxBooleanValue(this, bool);
  }

  @Override
  public NullValue<OrthodoxValue> nullValue () {

    return new OrthodoxNullValue(this);
  }
}
