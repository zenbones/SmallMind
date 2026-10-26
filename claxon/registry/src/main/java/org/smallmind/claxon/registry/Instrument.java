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
package org.smallmind.claxon.registry;

import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;

/**
 * Static façade that allows application code to obtain {@link Instrumentation} instances
 * without holding an explicit reference to a {@link ClaxonRegistry}.
 *
 * <p>A registry must be installed via {@link #register(ClaxonRegistry)} before
 * {@link #with(Class, MeterBuilder, Tag...)} can return active instrumentation.
 * Until a registry is installed — or after it has been removed — every call to
 * {@code with} returns an {@link UnpluggedInstrumentation} that silently discards all
 * metric updates, ensuring that instrumented code paths never throw a
 * {@link NullPointerException} due to a missing registry.
 *
 * <p>The registry is stored through {@link ClaxonRegistryManager}, which keys it by the
 * {@code PerApplicationScope} or {@code PerApplicationContext} bound on the calling thread (scope first,
 * then context), so that multi-application environments can maintain an independent registry per
 * binding. Lookups never throw: {@link #with} returns the {@link UnpluggedInstrumentation} both when
 * nothing has been installed and when the calling thread carries neither binding, whereas
 * {@link #register} fails with {@code MissingPerApplicationContextException} on a thread with no binding.
 */
public class Instrument {

  /**
   * Shared no-op instrumentation returned when no registry has been installed.
   */
  private static final UnpluggedInstrumentation UNPLUGGED_INSTRUMENTATION = new UnpluggedInstrumentation();

  /**
   * Installs {@code registry} as the per-application {@link ClaxonRegistry} so that
   * subsequent calls to {@link #with} can resolve meters through it.
   *
   * @param registry the registry to install for the current application context
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void register (ClaxonRegistry registry) {

    ClaxonRegistryManager.register(registry);
  }

  /**
   * Returns the {@link ClaxonRegistry} currently installed for the active application
   * context, or {@code null} if none has been installed.
   *
   * @return the installed {@link ClaxonRegistry}, or {@code null} if none has been installed or if no per-application
   * scope or context is bound on the calling thread
   */
  public static ClaxonRegistry getRegistry () {

    return ClaxonRegistryManager.getRegistry();
  }

  /**
   * Creates an {@link Instrumentation} that routes metric updates through the currently
   * installed registry for the given caller class, builder, and tags.
   *
   * <p>If no registry is installed, an {@link UnpluggedInstrumentation} is returned
   * instead; it accepts all calls without performing any measurement, so call-site
   * code does not need to guard against a missing registry.
   *
   * @param caller  the class that is requesting instrumentation, used for meter naming
   * @param builder the meter builder that describes the type of meter to create
   * @param tags    optional tags to associate with the meter; may be empty
   * @return a {@link WorkingInstrumentation} backed by the active registry, or
   * an {@link UnpluggedInstrumentation} if no registry is installed
   */
  public static Instrumentation with (Class<?> caller, MeterBuilder<? extends Meter> builder, Tag... tags) {

    ClaxonRegistry registry;

    return ((registry = getRegistry()) != null) ? new WorkingInstrumentation(registry, caller, builder, tags) : UNPLUGGED_INSTRUMENTATION;
  }
}
