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
package org.smallmind.nutsnbolts.command.template;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Describes a command line option including its name/flag, required status, children, and argument definition.
 */
public class Option {

  private final String name;
  private final Character flag;
  private final boolean required;
  private LinkedList<Option> children;
  private Option parent;
  private Argument argument;

  /**
   * Creates an option with no parent and optional child options.
   *
   * @param name     long option name (may be {@code null})
   * @param flag     single-character flag (may be {@code null})
   * @param required whether the option must be provided
   * @param children child options that depend on this option
   */
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

  /**
   * Creates an option with an argument definition and optional children.
   *
   * @param name     long option name (may be {@code null})
   * @param flag     single-character flag (may be {@code null})
   * @param required whether the option must be provided
   * @param argument argument definition for the option
   * @param children child options that depend on this option
   */
  public Option (String name, Character flag, boolean required, Argument argument, Option... children) {

    this(name, flag, required, children);

    this.argument = argument;
  }

  /**
   * Creates a child option linked to a parent.
   *
   * @param parent   parent option that must be present
   * @param name     long option name
   * @param flag     single-character flag
   * @param required whether the option must be provided
   * @param children nested dependent options
   */
  public Option (Option parent, String name, Character flag, boolean required, Option... children) {

    this(name, flag, required, children);

    this.parent = parent;
  }

  /**
   * Creates a child option with an argument definition.
   *
   * @param parent   parent option that must be present
   * @param name     long option name
   * @param flag     single-character flag
   * @param required whether the option must be provided
   * @param argument argument definition
   * @param children nested dependent options
   */
  public Option (Option parent, String name, Character flag, boolean required, Argument argument, Option... children) {

    this(name, flag, required, argument, children);

    this.parent = parent;
  }

  /**
   * @return long option name or {@code null}
   */
  public String getName () {

    return name;
  }

  /**
   * @return flag character or {@code null}
   */
  public Character getFlag () {

    return flag;
  }

  /**
   * @return {@code true} if the option is required
   */
  public boolean isRequired () {

    return required;
  }

  /**
   * @return parent option or {@code null} if root
   */
  public Option getParent () {

    return parent;
  }

  /**
   * Sets the parent option dependency.
   *
   * @param parent parent option
   */
  public void setParent (Option parent) {

    this.parent = parent;
  }

  /**
   * @return child options that depend on this option
   */
  public LinkedList<Option> getChildren () {

    return children;
  }

  /**
   * Defines child options, replacing any existing list.
   *
   * @param children new child option list
   */
  public void setChildren (LinkedList<Option> children) {

    this.children = children;
  }

  /**
   * @return argument definition or {@code null} if the option accepts none
   */
  public Argument getArgument () {

    return argument;
  }

  /**
   * Assigns the argument definition for the option.
   *
   * @param argument argument model
   */
  public void setArgument (Argument argument) {

    this.argument = argument;
  }
}
