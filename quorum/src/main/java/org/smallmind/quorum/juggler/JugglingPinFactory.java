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
package org.smallmind.quorum.juggler;

/**
 * Strategy for constructing {@link JugglingPin} instances from provider objects.
 * <p>
 * A {@link Juggler} holds one factory and calls {@link #createJugglingPin} once per provider
 * during {@link Juggler#initialize()}, producing the full set of pins that the juggler will
 * manage. Implementations are responsible for connecting or otherwise initialising the
 * provider into a form the pin can later {@link JugglingPin#obtain() obtain} and serve.
 *
 * @param <P> the type of provider used to back each pin
 * @param <R> the type of resource the resulting pin will expose
 */
public interface JugglingPinFactory<P, R> {

  /**
   * Constructs a new {@link JugglingPin} that wraps the given provider.
   *
   * @param provider      the provider instance from which the pin's resource is derived
   * @param resourceClass the runtime class of the resource, supplied so implementations
   *                      may perform reflective or proxy-based resource creation
   * @return a fully constructed pin ready for lifecycle management by the {@link Juggler}
   * @throws JugglerResourceCreationException if the pin or its underlying resource cannot be instantiated
   */
  JugglingPin<R> createJugglingPin (P provider, Class<R> resourceClass)
    throws JugglerResourceCreationException;
}
