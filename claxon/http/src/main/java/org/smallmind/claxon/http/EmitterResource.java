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

/**
 * JAX-RS resource that exposes pull-style Claxon emitters over HTTP.
 *
 * <p>Each emitter registered under a given name in the {@link ClaxonRegistry} can be
 * retrieved by issuing an HTTP GET to {@code /org/smallmind/claxon/emitter/{name}}. Only
 * emitters whose collection method is {@link EmitterMethod#PULL} are eligible; attempts to
 * access push emitters via this endpoint result in an {@link InvalidEmitterException}.
 */
@Path("/org/smallmind/claxon/emitter")
public class EmitterResource {

  /**
   * Registry used to look up registered emitters by name.
   */
  private ClaxonRegistry registry;

  /**
   * No-argument constructor for use by dependency-injection frameworks and JAX-RS runtimes
   * that require a public no-arg constructor.
   */
  public EmitterResource () {

  }

  /**
   * Creates a resource bound to the given registry.
   *
   * @param registry the {@link ClaxonRegistry} to query when resolving emitter names
   */
  public EmitterResource (ClaxonRegistry registry) {

    this.registry = registry;
  }

  /**
   * Sets the registry used to resolve emitters by name.
   *
   * @param registry the {@link ClaxonRegistry} to use; must not be {@code null} at request time
   */
  public void setRegistry (ClaxonRegistry registry) {

    this.registry = registry;
  }

  /**
   * Fetches the current output of a named pull emitter.
   *
   * <p>The emitter identified by {@code name} is looked up in the configured registry. If
   * found and of type {@link EmitterMethod#PULL}, its {@link PullEmitter#emit()} method is
   * invoked and the result is returned as an HTTP 200 response body.
   *
   * @param name the name of the registered emitter to invoke
   * @return an HTTP {@link Response} whose entity is the value produced by the emitter
   * @throws UnknownEmitterException if no emitter with the given name is registered
   * @throws InvalidEmitterException if the emitter exists but its collection method is not
   *                                 {@link EmitterMethod#PULL}
   */
  @GET
  @Path("/{name}")
  public Response get (@PathParam("name") String name)
    throws UnknownEmitterException, InvalidEmitterException {

    Emitter emitter;

    if ((emitter = registry.getEmitter(name)) == null) {
      throw new UnknownEmitterException(name);
    } else if (!EmitterMethod.PULL.equals(emitter.getEmitterMethod())) {
      throw new InvalidEmitterException("Invalid collection method(%s) for emitter(%s)", emitter.getEmitterMethod(), name);
    } else {

      return Response.ok(((PullEmitter<?>)emitter).emit()).build();
    }
  }
}
