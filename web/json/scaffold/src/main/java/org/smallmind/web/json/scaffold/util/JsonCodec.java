/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.web.json.scaffold.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonCodec {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModules(new JaxbAnnotationModule().setNonNillableInclusion(JsonInclude.Include.NON_NULL), new PolymorphicModule()).configure(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME, true);

  public static <T> T read (byte[] bytes, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(bytes, clazz);
  }

  public static <T> T read (byte[] bytes, int offset, int len, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(bytes, offset, len, clazz);
  }

  public static <T> T read (String aString, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(aString, clazz);
  }

  public static <T> T read (InputStream inputStream, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(inputStream, clazz);
  }

  public static <T> T read (JsonParser parser, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(parser, clazz);
  }

  public static JsonNode writeAsJsonNode (Object obj)
    throws JsonProcessingException {

    return OBJECT_MAPPER.valueToTree(obj);
  }

  public static byte[] writeAsBytes (Object obj)
    throws JsonProcessingException {

    return OBJECT_MAPPER.writeValueAsBytes(obj);
  }

  public static String writeAsString (Object obj)
    throws JsonProcessingException {

    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  public static void writeToStream (OutputStream outputStream, Object obj)
    throws IOException {

    OBJECT_MAPPER.writeValue(outputStream, obj);
  }

  public static <T> T convert (Object obj, Class<T> clazz) {

    return OBJECT_MAPPER.convertValue(obj, clazz);
  }
}
