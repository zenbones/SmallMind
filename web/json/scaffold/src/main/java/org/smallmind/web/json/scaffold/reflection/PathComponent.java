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
package org.smallmind.web.json.scaffold.reflection;

import java.io.IOException;
import java.util.LinkedList;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class PathComponent {

  private final String name;
  private LinkedList<Integer> subscriptList;
  private Object[] arguments;

  public PathComponent (String name) {

    this.name = name;
  }

  public String getName () {

    return name;
  }

  public Object[] getArguments () {

    return arguments;
  }

  public void createArguments (String argumentSubPath)
    throws IOException {

    arguments = JsonCodec.read('[' + argumentSubPath.strip() + ']', Object[].class);
  }

  public int[] getSubscripts () {

    if ((subscriptList == null) || subscriptList.isEmpty()) {

      return null;
    } else {

      int[] subscripts = new int[subscriptList.size()];
      int index = 0;

      for (Integer subscript : subscriptList) {
        subscripts[index++] = subscript;
      }

      return subscripts;
    }
  }

  public void addSubscript (String subscriptSubPath) {

    if (subscriptList == null) {
      subscriptList = new LinkedList<>();
    }
    subscriptList.add(Integer.parseInt(subscriptSubPath.strip()));
  }
}
