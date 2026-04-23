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

import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;

/**
 * Represents a named time window used by meters such as the trace meter to define rolling
 * measurement intervals. The {@code name} is a human-readable label (e.g., {@code "m1"},
 * {@code "m5"}) and the {@code value} is the numeric duration expressed in the unit
 * configured on the enclosing meter properties (typically minutes). Instances are
 * populated by the JSON doppelganger framework during deserialization.
 */
@Doppelganger(namespace = "http://org.smallmind/claxon/registry")
public class Window {

  /**
   * Human-readable label identifying this window (e.g., {@code "m1"} for a one-minute window).
   */
  @View(idioms = @Idiom(visibility = IN))
  private String name;

  /**
   * Numeric duration of the window expressed in the time unit configured on the enclosing
   * properties object (e.g., {@code 1} combined with {@code TimeUnit.MINUTES} yields a
   * one-minute window).
   */
  @View(idioms = @Idiom(visibility = IN))
  private long value;

  /**
   * No-argument constructor required by the JSON doppelganger framework for reflective
   * instantiation during deserialization.
   */
  public Window () {

  }

  /**
   * Constructs a window with the given name and duration value.
   *
   * @param name  human-readable label for the window (e.g., {@code "m5"})
   * @param value numeric duration in the unit defined by the enclosing configuration
   */
  public Window (String name, long value) {

    this.name = name;
    this.value = value;
  }

  /**
   * Returns the human-readable label for this window.
   *
   * @return the window name (e.g., {@code "m1"}, {@code "m15"})
   */
  public String getName () {

    return name;
  }

  /**
   * Sets the human-readable label for this window.
   *
   * @param name the window name (e.g., {@code "m5"})
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Returns the numeric duration of this window.
   *
   * @return the duration value in the unit defined by the enclosing configuration
   */
  public long getValue () {

    return value;
  }

  /**
   * Sets the numeric duration of this window.
   *
   * @param value the duration in the unit defined by the enclosing configuration
   */
  public void setValue (long value) {

    this.value = value;
  }
}
