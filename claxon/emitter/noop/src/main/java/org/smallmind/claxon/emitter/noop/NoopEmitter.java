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
package org.smallmind.claxon.emitter.noop;

import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * Push emitter that silently discards all incoming metrics without performing any I/O or
 * side effects.
 *
 * <p>This implementation is useful as a placeholder in environments where metric emission is
 * intentionally disabled, or during testing when actual metric transport is not desired.
 */
public class NoopEmitter extends PushEmitter {

  /**
   * Creates a no-operation emitter.
   */
  public NoopEmitter () {

  }

  /**
   * Accepts metric data and discards it without performing any action.
   *
   * @param meterName  the name of the meter (ignored)
   * @param tags       the associated tags (ignored)
   * @param quantities the measured values (ignored)
   */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities) {

  }
}
