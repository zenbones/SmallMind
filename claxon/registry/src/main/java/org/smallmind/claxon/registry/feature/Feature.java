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
package org.smallmind.claxon.registry.feature;

import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * Contract for a periodically sampled metric source managed by a {@link org.smallmind.claxon.registry.ClaxonRegistry}.
 * A feature encapsulates a self-contained unit of measurable state that the registry will
 * poll on a regular schedule. Each invocation of {@link #record()} should capture a
 * consistent snapshot of the feature's current measurements and return them as an array of
 * {@link Quantity} objects, which the registry then forwards to the configured emitters.
 *
 * <p>Implementations must be thread-safe because {@link #record()} may be invoked from a
 * registry-managed background thread concurrently with other operations on the feature.
 */
public interface Feature {

  /**
   * Returns the logical name of the meter associated with this feature. The name is used by
   * the registry to identify and group measurements from this feature within the metric
   * namespace.
   *
   * @return the feature's meter name; must not be {@code null} or empty
   */
  String getName ();

  /**
   * Returns the dimensional tags that should be applied to all {@link Quantity} values
   * emitted by this feature. Tags allow emitters to attach metadata such as environment,
   * region, or service name to the recorded measurements.
   *
   * @return array of {@link Tag} instances; may be empty but must not be {@code null}
   */
  Tag[] getTags ();

  /**
   * Captures and returns the current measurements for this feature as a snapshot. This
   * method is invoked periodically by the registry on a background thread. Implementations
   * should collect all relevant metric values and return them as {@link Quantity} objects.
   *
   * @return array of {@link Quantity} objects representing the current state of the feature;
   * may be empty but must not be {@code null}
   */
  Quantity[] record ();
}
