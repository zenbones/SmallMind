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
package org.smallmind.web.jersey.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.Header;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class JsonHttpEntity extends AbstractHttpEntity {

  private static final Header CONTENT_TYPE_HEADER = new BasicHeader("Content-type", "application/json");
  private final byte[] bytes;

  public JsonHttpEntity (Object obj)
    throws JsonProcessingException {

    bytes = JsonCodec.writeAsBytes(obj);
  }

  public static JsonHttpEntity entity (Object obj)
    throws JsonProcessingException {

    return new JsonHttpEntity(obj);
  }

  @Override
  public boolean isRepeatable () {

    return true;
  }

  @Override
  public long getContentLength () {

    return bytes.length;
  }

  @Override
  public Header getContentType () {

    return CONTENT_TYPE_HEADER;
  }

  @Override
  public boolean isChunked () {

    return true;
  }

  @Override
  public InputStream getContent () {

    return new ByteArrayInputStream(bytes);
  }

  @Override
  public void writeTo (OutputStream outstream)
    throws IOException {

    outstream.write(bytes);
  }

  @Override
  public boolean isStreaming () {

    return false;
  }
}
