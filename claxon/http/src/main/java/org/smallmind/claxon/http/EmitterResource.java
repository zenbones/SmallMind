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
package org.smallmind.claxon.http;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.Emitter;
import org.smallmind.claxon.registry.EmitterMethod;
import org.smallmind.claxon.registry.InvalidEmitterException;
import org.smallmind.claxon.registry.PullEmitter;
import org.smallmind.claxon.registry.UnknownEmitterException;

@Path("/org/smallmind/claxon/emitter")
public class EmitterResource {

  private ClaxonRegistry registry;

  public EmitterResource () {

  }

  public EmitterResource (ClaxonRegistry registry) {

    this.registry = registry;
  }

  public void setRegistry (ClaxonRegistry registry) {

    this.registry = registry;
  }

  @GET
  @Path("/{name}")
  public Response get (@PathParam("name") String name)
    throws UnknownEmitterException, InvalidEmitterException {

    Emitter emitter;

    if ((emitter = registry.getEmitter(name)) == null) {
      throw new UnknownEmitterException(name);
    } else if (!EmitterMethod.PULL.equals(emitter.getEmitterMethod())) {
      throw new InvalidEmitterException("Invalid collection method(%s) for emitter(%s)");
    } else {

      return Response.ok(((PullEmitter<?>)emitter).emit()).build();
    }
  }
}
