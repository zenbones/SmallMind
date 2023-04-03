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
package org.smallmind.mongodb.throng;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

public class ThrongClient {

  private final HashMap<Class<?>, MongoCollection> collectionMap = new HashMap<>();
  private final MongoDatabase mongoDatabase;
  private final CodecRegistry codecRegistry;

  // mongoDatabase.getCollection("collection").withCodecRegistry(codecRegistry).withDocumentClass(ThrongDocument.class);

  public ThrongClient (MongoClient mongoClient, String database, Class<?>... entityClasses)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    LinkedList<Codec<?>> throngCodecList = new LinkedList<>();

    mongoDatabase = mongoClient.getDatabase(database);

    if (entityClasses != null) {

      HashMap<String, ThrongEmbeddedCodec<?>> embeddedReferenceMap = new HashMap<>();

      for (Class<?> entityClass : entityClasses) {
        throngCodecList.add(new ThrongEntityCodec<>(entityClass, new ThrongEntity(entityClass, mongoDatabase.getCodecRegistry(), embeddedReferenceMap)));
      }

      throngCodecList.addAll(embeddedReferenceMap.values());
    }

    codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new ThrongDocumentCodec()), CodecRegistries.fromCodecs(throngCodecList), mongoDatabase.getCodecRegistry());
  }

  public CodecRegistry getCodecRegistry () {

    return codecRegistry;
  }
}
