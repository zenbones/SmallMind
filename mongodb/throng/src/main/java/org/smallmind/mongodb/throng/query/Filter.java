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
package org.smallmind.mongodb.throng.query;

import java.util.regex.Pattern;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.smallmind.nutsnbolts.util.MutationUtility;

public class Filter {

  private final String fieldName;
  private Bson bson;

  private Filter (Bson bson) {

    this.bson = bson;

    fieldName = "";
  }

  public Filter (String fieldName) {

    this.fieldName = fieldName;
  }

  public static Filter where (String fieldName) {

    return new Filter(fieldName);
  }

  public static Filter and (Filter... filters) {

    if ((filters == null) || (filters.length == 0)) {

      return new Filter(com.mongodb.client.model.Filters.empty());
    } else {

      return new Filter(com.mongodb.client.model.Filters.and(MutationUtility.toArray(filters, Bson.class, filter -> filter.bson)));
    }
  }

  public static Filter or (Filter... filters) {

    if ((filters == null) || (filters.length == 0)) {

      return new Filter(com.mongodb.client.model.Filters.empty());
    } else {

      return new Filter(com.mongodb.client.model.Filters.or(MutationUtility.toArray(filters, Bson.class, filter -> filter.bson)));
    }
  }

  public Filter eq (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.eq(fieldName, value);

      return this;
    }
  }

  public Filter ne (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.ne(fieldName, value);

      return this;
    }
  }

  public Filter gt (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.gt(fieldName, value);

      return this;
    }
  }

  public Filter gte (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.gte(fieldName, value);

      return this;
    }
  }

  public Filter lt (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.lt(fieldName, value);

      return this;
    }
  }

  public Filter lte (Object value) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.lte(fieldName, value);

      return this;
    }
  }

  public Filter exists (boolean exists) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.exists(fieldName, exists);

      return this;
    }
  }

  public Filter in (Object... values) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.in(fieldName, values);

      return this;
    }
  }

  public Filter nin (Object... values) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.nin(fieldName, values);

      return this;
    }
  }

  public Filter regex (Pattern pattern) {

    if (bson != null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.regex(fieldName, pattern);

      return this;
    }
  }

  public Filter not () {

    if (bson == null) {
      throw new UnsupportedOperationException();
    } else {

      bson = com.mongodb.client.model.Filters.not(bson);

      return this;
    }
  }

  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    if (bson == null) {
      throw new UnsupportedOperationException();
    } else {

      return bson.toBsonDocument(documentClass, codecRegistry);
    }
  }
}
