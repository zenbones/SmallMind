/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jersey.jackson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.smallmind.web.jersey.util.JsonCodec;

@Provider
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
