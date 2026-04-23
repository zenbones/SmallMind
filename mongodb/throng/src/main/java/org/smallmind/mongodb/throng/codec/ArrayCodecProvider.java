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
package org.smallmind.mongodb.throng.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Driver {@link CodecProvider} that supplies an {@link ArrayCodec} for any Java array type whose component
 * type has a codec available in the registry.
 */
public class ArrayCodecProvider implements CodecProvider {

  private final boolean storeNulls;

  /**
   * Constructs the provider with the given null-storage setting.
   *
   * @param storeNulls {@code true} to write BSON null when an array value is {@code null}
   */
  public ArrayCodecProvider (boolean storeNulls) {

    this.storeNulls = storeNulls;
  }

  /**
   * Returns an {@link ArrayCodec} when the requested type is an array and the registry can supply a codec for
   * its component type; returns {@code null} otherwise.
   *
   * @param clazz    the type requested from the codec registry
   * @param registry the codec registry used to resolve the component codec
   * @param <T>      the requested type
   * @return a configured {@link ArrayCodec}, or {@code null} if the type is not a supported array
   */
  @Override
  public <T> Codec<T> get (Class<T> clazz, CodecRegistry registry) {

    if (clazz.isArray()) {

      Codec<?> itemCodec;

      if ((itemCodec = registry.get(clazz.getComponentType())) != null) {

        return new ArrayCodec<>(clazz, clazz.getComponentType(), itemCodec, storeNulls);
      }
    }

    return null;
  }
}
