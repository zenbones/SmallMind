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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;
import org.smallmind.web.json.scaffold.fault.NativeLanguage;
import org.smallmind.web.json.scaffold.fault.NativeObject;
import org.smallmind.web.json.scaffold.fault.ObjectInstantiationException;
import org.smallmind.web.json.scaffold.fault.ResourceInvocationException;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class FaultTranslatingClientResponseFilter implements ClientResponseFilter {

  private final int lowerBound;
  private final int upperBound;

  public FaultTranslatingClientResponseFilter () {

    this(400, 600);
  }

  public FaultTranslatingClientResponseFilter (int lowerBound, int upperBound) {

    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  @Override
  public void filter (ClientRequestContext requestContext, ClientResponseContext responseContext)
    throws IOException {

    if ((responseContext.getStatus() >= lowerBound) && (responseContext.getStatus() < upperBound) && MediaType.APPLICATION_JSON_TYPE.equals(responseContext.getMediaType()) && responseContext.hasEntity()) {

      Fault fault;
      NativeObject nativeObject;

      if (((nativeObject = (fault = JsonCodec.read(responseContext.getEntityStream(), Fault.class)).getNativeObject()) != null) && nativeObject.getLanguage().equals(NativeLanguage.JAVA)) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(nativeObject.getBytes()); ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
          try {
            throw new ResourceInvocationException((Throwable)objectInputStream.readObject());
          } catch (ClassNotFoundException classNotFoundException) {
            throw new ObjectInstantiationException(classNotFoundException);
          }
        }
      }

      throw new FaultWrappingException(fault);
    }
  }
}
