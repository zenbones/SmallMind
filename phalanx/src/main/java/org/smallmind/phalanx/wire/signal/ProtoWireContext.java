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
package org.smallmind.phalanx.wire.signal;

/**
 * Wire context used when the concrete type is unknown, carrying raw JSON payload and the original tag.
 */
public class ProtoWireContext extends WireContext {

  private final Object guts;
  private final String skin;

  /**
   * Creates a proto context with a tag and raw content.
   *
   * @param skin name identifying the context type
   * @param guts payload to be deserialized later
   */
  public ProtoWireContext (String skin, Object guts) {

    this.skin = skin;
    this.guts = guts;
  }

  /**
   * Returns the raw payload.
   *
   * @return untyped context contents
   */
  public Object getGuts () {

    return guts;
  }

  /**
   * Returns the tag used to identify the context type.
   *
   * @return context name
   */
  public String getSkin () {

    return skin;
  }
}
