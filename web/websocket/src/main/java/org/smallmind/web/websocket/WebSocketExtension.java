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
package org.smallmind.web.websocket;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.websocket.Extension;

/**
 * Simple immutable implementation of a WebSocket extension definition with optional parameters.
 */
public class WebSocketExtension implements Extension {

  private final String name;
  private final ExtensionParameter[] parameters;

  /**
   * Builds an extension declaration.
   *
   * @param name the extension name
   * @param parameters optional parameters associated with the extension
   */
  public WebSocketExtension (String name, ExtensionParameter... parameters) {

    this.name = name;
    this.parameters = parameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName () {

    return name;
  }

  /**
   * Internal helper returning the parameter array.
   *
   * @return extension parameters or {@code null}
   */
  private ExtensionParameter[] getParametersAsArray () {

    return parameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Parameter> getParameters () {

    if (parameters == null) {

      return Collections.emptyList();
    }

    return Arrays.asList(parameters);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode () {

    return name.hashCode() ^ Arrays.hashCode(parameters);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof WebSocketExtension) && ((WebSocketExtension)obj).getName().equals(name) && Arrays.equals(((WebSocketExtension)obj).getParametersAsArray(), parameters);
  }
}
