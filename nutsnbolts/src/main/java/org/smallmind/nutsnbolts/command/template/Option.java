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
package org.smallmind.nutsnbolts.command.template;

import java.util.Arrays;
import java.util.LinkedList;

public class Option {

  private final String name;
  private final Character flag;
  private final boolean required;
  private LinkedList<Option> children;
  private Option parent;
  private Argument argument;

  public Option (String name, Character flag, boolean required, Option... children) {

    this.name = name;
    this.flag = flag;
    this.required = required;

    if (children != null) {
      this.children = new LinkedList<>(Arrays.asList(children));

      for (Option childOption : this.children) {
        childOption.setParent(this);
      }
    }
  }

  public Option (String name, Character flag, boolean required, Argument argument, Option... children) {

    this(name, flag, required, children);

    this.argument = argument;
  }

  public Option (Option parent, String name, Character flag, boolean required, Option... children) {

    this(name, flag, required, children);

    this.parent = parent;
  }

  public Option (Option parent, String name, Character flag, boolean required, Argument argument, Option... children) {

    this(name, flag, required, argument, children);

    this.parent = parent;
  }

  public String getName () {

    return name;
  }

  public Character getFlag () {

    return flag;
  }

  public boolean isRequired () {

    return required;
  }

  public Option getParent () {

    return parent;
  }

  public void setParent (Option parent) {

    this.parent = parent;
  }

  public LinkedList<Option> getChildren () {

    return children;
  }

  public void setChildren (LinkedList<Option> children) {

    this.children = children;
  }

  public Argument getArgument () {

    return argument;
  }

  public void setArgument (Argument argument) {

    this.argument = argument;
  }
}
