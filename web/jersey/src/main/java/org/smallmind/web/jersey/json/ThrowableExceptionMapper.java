/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

  private ExceptionMapper[] mappers;
  private boolean logUnclassifiedErrors;

  public ThrowableExceptionMapper (ExceptionMapper... mappers) {

    this(false, mappers);
  }

  public ThrowableExceptionMapper (boolean logUnclassifiedErrors, ExceptionMapper... mappers) {

    this.logUnclassifiedErrors = logUnclassifiedErrors;
    this.mappers = mappers;
  }

  @Override
  public Response toResponse (Throwable throwable) {

    if (mappers != null) {
      for (ExceptionMapper mapper : mappers) {

        Class<? extends Throwable> mappedThrowableClass = (Class<? extends Throwable>)GenericUtility.getTypeArguments(ConcreteExceptionMapper.class, mapper.getClass()).get(0);

        if (mappedThrowableClass.isAssignableFrom(throwable.getClass())) {

          Response response;

          if ((response = mapper.toResponse(throwable)) != null) {

            return response;
          }
        }
      }
    }

    if (throwable instanceof WebApplicationException) {
      if (logUnclassifiedErrors) {

        Response response;

        if (((response = ((WebApplicationException)throwable).getResponse()) != null) && (response.getStatus() == 500)) {
          LoggerManager.getLogger(ThrowableExceptionMapper.class).error(throwable);
        }
      }

      return ((WebApplicationException)throwable).getResponse();
    }

    if (logUnclassifiedErrors) {
      LoggerManager.getLogger(ThrowableExceptionMapper.class).error(throwable);
    }

    return Response.status(500).type(MediaType.APPLICATION_JSON).entity((throwable instanceof FaultWrappingException) ? ((FaultWrappingException)throwable).getFault() : new Fault(throwable)).build();
  }
}
