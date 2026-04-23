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
package org.smallmind.memcached.cubby.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * {@link CubbyCodec} implementation that uses standard Java object serialization streams
 * ({@link ObjectOutputStream} / {@link ObjectInputStream}) to encode and decode values.
 * During deserialization the thread context class loader is tried as a fallback when the
 * default stream resolver cannot locate the target class, making this codec suitable for
 * use in environments with non-standard class-loading hierarchies (e.g., OSGi, web
 * containers).
 */
public class ObjectStreamCubbyCodec implements CubbyCodec {

  /**
   * Serializes the supplied object to a byte array using a Java {@link ObjectOutputStream}.
   *
   * @param obj the object to serialize; must not be {@code null}
   * @return the serialized byte representation of {@code obj}
   * @throws IOException if an I/O error occurs while writing to the object stream
   */
  @Override
  public byte[] serialize (Object obj)
    throws IOException {

    if (obj == null) {
      throw new NullPointerException("Can not serialize a null value");
    } else {

      ByteArrayOutputStream byteStream;

      try (ObjectOutputStream out = new ObjectOutputStream(byteStream = new ByteArrayOutputStream())) {
        out.writeObject(obj);
      }

      return byteStream.toByteArray();
    }
  }

  /**
   * Deserializes the supplied byte array into an object using a Java
   * {@link ObjectInputStream} that falls back to the thread context class loader when
   * class resolution fails via the default mechanism.
   *
   * @param bytes the byte array to deserialize
   * @return the deserialized object
   * @throws IOException            if an I/O error occurs while reading the object stream
   * @throws ClassNotFoundException if the class of the serialized object cannot be found
   *                                even after consulting the thread context class loader
   */
  @Override
  public Object deserialize (byte[] bytes)
    throws IOException, ClassNotFoundException {

    try (ResolvingObjectInputStream in = new ResolvingObjectInputStream(new ByteArrayInputStream(bytes))) {

      return in.readObject();
    }
  }

  /**
   * An {@link ObjectInputStream} subclass that supplements the default class-resolution
   * strategy with a lookup against the current thread's context class loader. This allows
   * deserialization of classes that are not visible to the system or bootstrap class loader
   * but are accessible to the deploying application.
   */
  private static final class ResolvingObjectInputStream extends ObjectInputStream {

    /**
     * Constructs a resolving input stream that reads serialized data from the given stream.
     *
     * @param in the underlying input stream providing serialized bytes
     * @throws IOException if the stream cannot be initialized
     */
    public ResolvingObjectInputStream (InputStream in)
      throws IOException {

      super(in);
    }

    /**
     * Resolves the class described by {@code desc}, falling back to the thread context
     * class loader when the default resolution throws {@link ClassNotFoundException}.
     *
     * @param desc the {@link ObjectStreamClass} descriptor for the class to resolve
     * @return the resolved {@link Class} object
     * @throws IOException            if an I/O error occurs during resolution
     * @throws ClassNotFoundException if the class cannot be found by either the default
     *                                resolver or the thread context class loader
     */
    @Override
    protected Class<?> resolveClass (ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {

      try {

        return super.resolveClass(desc);
      } catch (ClassNotFoundException classNotFoundException) {

        return Thread.currentThread().getContextClassLoader().loadClass(desc.getName());
      }
    }
  }
}
