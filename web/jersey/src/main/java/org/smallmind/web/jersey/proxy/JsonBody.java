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
package org.smallmind.web.jersey.proxy;

import org.apache.hc.core5.http.ContentType;
import org.smallmind.web.jersey.aop.Envelope;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Holds the serialized JSON byte payload and its content type for use in proxied HTTP requests.
 */
public class JsonBody {

  private final byte[] bodyAsBytes;
  private final ContentType contentType;

  /**
   * Creates a body by encoding a raw JSON string to bytes.
   *
   * @param json JSON string to send as the request body
   */
  public JsonBody (String json) {

    this.bodyAsBytes = json.getBytes();
    this.contentType = ContentType.APPLICATION_JSON;
  }

  /**
   * Creates a body by serializing an {@link Envelope} with {@link JsonCodec}.
   *
   * @param envelope envelope to serialize
   */
  public JsonBody (Envelope envelope) {

    this.bodyAsBytes = JsonCodec.writeAsBytes(envelope);
    this.contentType = ContentType.APPLICATION_JSON;
  }

  /**
   * Creates a body by serializing an arbitrary object with {@link JsonCodec}.
   *
   * @param obj object to serialize
   */
  public JsonBody (Object obj) {

    this.bodyAsBytes = JsonCodec.writeAsBytes(obj);
    this.contentType = ContentType.APPLICATION_JSON;
  }

  /**
   * Returns the content type, which is always {@code application/json}.
   *
   * @return content type
   */
  public ContentType getContentType () {

    return contentType;
  }

  /**
   * Returns the serialized JSON payload.
   *
   * @return body as a byte array
   */
  public byte[] getBodyAsBytes () {

    return bodyAsBytes;
  }
}
