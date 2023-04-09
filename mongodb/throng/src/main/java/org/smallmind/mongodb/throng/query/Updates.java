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

import java.util.LinkedList;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

public class Updates {

  private final LinkedList<Bson> updateList = new LinkedList<>();

  public Updates set (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.set(fieldName, value));

    return this;
  }

  public Updates unset (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.unset(fieldName));

    return this;
  }

  public Updates setOnInsert (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.setOnInsert(fieldName, value));

    return this;
  }

  public Updates inc (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.inc(fieldName, number));

    return this;
  }

  public Updates mul (String fieldName, Number number) {

    updateList.add(com.mongodb.client.model.Updates.mul(fieldName, number));

    return this;
  }

  public Updates max (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.max(fieldName, value));

    return this;
  }

  public Updates min (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.min(fieldName, value));

    return this;
  }

  public Updates push (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.push(fieldName, value));

    return this;
  }

  public Updates addToSet (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.addToSet(fieldName, value));

    return this;
  }

  public Updates pull (String fieldName, Object value) {

    updateList.add(com.mongodb.client.model.Updates.pull(fieldName, value));

    return this;
  }

  public Updates popFirst (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popFirst(fieldName));

    return this;
  }

  public Updates popLast (String fieldName) {

    updateList.add(com.mongodb.client.model.Updates.popLast(fieldName));

    return this;
  }

  public Updates rename (String fieldName, String updatedFieldName) {

    updateList.add(com.mongodb.client.model.Updates.rename(fieldName, updatedFieldName));

    return this;
  }

  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    return com.mongodb.client.model.Updates.combine(updateList).toBsonDocument(documentClass, codecRegistry);
  }
}
