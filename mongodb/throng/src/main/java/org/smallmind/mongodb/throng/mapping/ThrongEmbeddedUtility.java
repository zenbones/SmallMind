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
package org.smallmind.mongodb.throng.mapping;

import java.lang.reflect.InvocationTargetException;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.lifecycle.ThrongLifecycleRejection;
import org.smallmind.mongodb.throng.mapping.annotation.Embedded;
import org.smallmind.mongodb.throng.mapping.annotation.Polymorphic;

/**
 * Factory utility for generating and caching codecs for {@code @Embedded} types.
 *
 * <p>A single call to {@link #generateEmbeddedCodec} produces a {@link ThrongPolymorphicEmbeddedCodec}
 * when the {@code @Embedded} annotation declares subtypes, or a plain {@link ThrongEmbeddedCodec}
 * otherwise. Results are stored in the supplied {@link EmbeddedReferences} cache so that the same
 * codec instance is reused if the same type is encountered more than once during mapping setup.
 */
public class ThrongEmbeddedUtility {

  /**
   * Returns a codec for the given embedded type, creating and caching it on the first call and returning
   * the cached instance on subsequent calls for the same type.
   *
   * <p>Produces a {@link ThrongPolymorphicEmbeddedCodec} when the {@code @Embedded} annotation declares
   * one or more subtypes, or a {@link ThrongEmbeddedCodec} for a non-polymorphic embed.
   *
   * @param embeddedType       the embedded class for which a codec is required
   * @param embedded           the {@code @Embedded} annotation on the class
   * @param codecRegistry      registry used to resolve codecs for the embedded type's own properties
   * @param embeddedReferences cache that stores generated codecs keyed by embedded class
   * @param storeNulls         {@code true} to encode null property values as BSON null
   * @return the codec for the embedded type; either a new instance or a previously cached one
   * @throws ThrongMappingException    if the embedded type declares lifecycle methods, which are not
   *                                   supported on embedded classes
   * @throws NoSuchMethodException     if the embedded type lacks a required no-arg constructor
   * @throws InstantiationException    if the embedded type cannot be instantiated
   * @throws IllegalAccessException    if a required constructor or field is not accessible
   * @throws InvocationTargetException if a constructor invoked during codec setup throws an exception
   */
  public static Codec<?> generateEmbeddedCodec (Class<?> embeddedType, Embedded embedded, CodecRegistry codecRegistry, EmbeddedReferences embeddedReferences, boolean storeNulls)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    Codec<?> codec;

    if ((codec = embeddedReferences.get(embeddedType)) == null) {

      Polymorphic polymorphic;

      // check for misuse of lifecycle annotations
      ThrongLifecycleRejection.reject(embeddedType);

      if ((polymorphic = embedded.polymorphic()).value().length > 0) {
        embeddedReferences.put(embeddedType, codec = new ThrongPolymorphicEmbeddedCodec<>(new ThrongPropertiesMultiplexer<>(embeddedType, polymorphic, codecRegistry, embeddedReferences, storeNulls)));
      } else {
        embeddedReferences.put(embeddedType, codec = new ThrongEmbeddedCodec<>(new ThrongProperties<>(embeddedType, codecRegistry, embeddedReferences, storeNulls)));
      }
    }

    return codec;
  }
}
