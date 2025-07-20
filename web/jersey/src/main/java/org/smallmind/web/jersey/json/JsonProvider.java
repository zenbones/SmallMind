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
package org.smallmind.web.jersey.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.smallmind.web.json.scaffold.util.JsonCodec;

@Priority(1)
@Consumes({MediaType.APPLICATION_JSON, "text/json"})
@Produces({MediaType.APPLICATION_JSON, "text/json"})
public class JsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

  private static final ThreadLocal<ByteArrayOutputStream> WRITE_BUFFER_LOCAL = new ThreadLocal<>();

  @Override
  public boolean isReadable (Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {

    return true;
  }

  @Override
  public Object readFrom (Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
    throws IOException, WebApplicationException {

    return JsonCodec.read(entityStream, type);
  }

  @Override
  public boolean isWriteable (Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {

    return true;
  }

  @Override
  public long getSize (Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {

    WRITE_BUFFER_LOCAL.set(new ByteArrayOutputStream());

    try {
      JsonCodec.writeToStream(WRITE_BUFFER_LOCAL.get(), o);

      return WRITE_BUFFER_LOCAL.get().size();
    } catch (Throwable throwable) {
      WRITE_BUFFER_LOCAL.remove();
      throw new WebApplicationException(throwable);
    }
  }

  @Override
  public void writeTo (Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
    throws IOException, WebApplicationException {

    try {
      if (WRITE_BUFFER_LOCAL.get() == null) {
        getSize(o, type, genericType, annotations, mediaType);
      }

      entityStream.write(WRITE_BUFFER_LOCAL.get().toByteArray());
    } finally {
      WRITE_BUFFER_LOCAL.remove();
    }
  }
}
