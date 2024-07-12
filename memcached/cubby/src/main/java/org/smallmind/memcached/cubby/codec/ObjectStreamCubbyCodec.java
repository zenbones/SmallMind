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
package org.smallmind.memcached.cubby.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

public class ObjectStreamCubbyCodec implements CubbyCodec {

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

  @Override
  public Object deserialize (byte[] bytes)
    throws IOException, ClassNotFoundException {

    try (ResolvingObjectInputStream in = new ResolvingObjectInputStream(new ByteArrayInputStream(bytes))) {

      return in.readObject();
    }
  }

  private static final class ResolvingObjectInputStream extends ObjectInputStream {

    public ResolvingObjectInputStream (InputStream in)
      throws IOException {

      super(in);
    }

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
