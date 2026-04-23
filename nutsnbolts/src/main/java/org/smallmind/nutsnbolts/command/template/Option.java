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
 * Describes a single command line option, including its long name, single-character flag, required status, argument definition, and any child options that depend upon it.
 */
public class Option {

  private final String name;
  private final Character flag;
  private final boolean required;
  private LinkedList<Option> children;
  private Option parent;
  private Argument argument;

  /**
   * Creates a root option identified by a long name and/or flag, with optional child options that implicitly depend on it.
   *
   * @param name     long option name used with {@code --}; may be {@code null} or empty if {@code flag} is set
   * @param flag     single-character flag used with {@code -}; may be {@code null} if {@code name} is set
   * @param required {@code true} if the option must appear on the command line
   * @param children zero or more child options whose parent is set to this option
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
   * Creates a root option with an argument definition and optional child options.
   *
   * @param name     long option name used with {@code --}; may be {@code null} or empty if {@code flag} is set
   * @param flag     single-character flag used with {@code -}; may be {@code null} if {@code name} is set
   * @param required {@code true} if the option must appear on the command line
   * @param argument argument definition describing how many values the option accepts
   * @param children zero or more child options whose parent is set to this option
   */
  public Option (String name, Character flag, boolean required, Argument argument, Option... children) {

    this(name, flag, required, children);

    this.argument = argument;
  }

  /**
   * Creates a child option that is linked to a parent option which must be present for this option to be valid.
   *
   * @param parent   parent option that must also be supplied on the command line
   * @param name     long option name used with {@code --}; may be {@code null} or empty if {@code flag} is set
   * @param flag     single-character flag used with {@code -}; may be {@code null} if {@code name} is set
   * @param required {@code true} if the option must appear on the command line
   * @param children zero or more grandchild options whose parent is set to this option
   */
  public Option (Option parent, String name, Character flag, boolean required, Option... children) {

    this(name, flag, required, children);

    this.parent = parent;
  }

  /**
   * Creates a child option linked to a parent, with an argument definition and optional grandchild options.
   *
   * @param parent   parent option that must also be supplied on the command line
   * @param name     long option name used with {@code --}; may be {@code null} or empty if {@code flag} is set
   * @param flag     single-character flag used with {@code -}; may be {@code null} if {@code name} is set
   * @param required {@code true} if the option must appear on the command line
   * @param argument argument definition describing how many values the option accepts
   * @param children zero or more grandchild options whose parent is set to this option
   */
  public Option (Option parent, String name, Character flag, boolean required, Argument argument, Option... children) {

    this(name, flag, required, argument, children);

    this.parent = parent;
  }

  /**
   * Returns the long name used with {@code --} on the command line.
   *
   * @return long option name, or {@code null} if not set
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the single-character flag used with {@code -} on the command line.
   *
   * @return flag character, or {@code null} if not set
   */
  public Character getFlag () {

    return flag;
  }

  /**
   * Indicates whether the option must be present on every invocation.
   *
   * @return {@code true} if the option is mandatory
   */
  public boolean isRequired () {

    return required;
  }

  /**
   * Returns the parent option that must be present when this option is used, or {@code null} for root options.
   *
   * @return parent {@link Option}, or {@code null} if this is a root option
   */
  public Option getParent () {

    return parent;
  }

  /**
   * Sets the parent option that this option depends upon.
   *
   * @param parent parent {@link Option} to associate with this option
   */
  public void setParent (Option parent) {

    this.parent = parent;
  }

  /**
   * Returns the list of child options that require this option to also be present.
   *
   * @return child option list, or {@code null} if no children were declared
   */
  public LinkedList<Option> getChildren () {

    return children;
  }

  /**
   * Replaces the list of child options with the supplied list.
   *
   * @param children new ordered list of child {@link Option}s
   */
  public void setChildren (LinkedList<Option> children) {

    this.children = children;
  }

  /**
   * Returns the argument definition describing how many values this option accepts.
   *
   * @return argument definition, or {@code null} if none has been assigned
   */
  public Argument getArgument () {

    return argument;
  }

  /**
   * Assigns the argument definition that governs how this option's values are parsed.
   *
   * @param argument argument definition to associate with this option
   */
  public void setArgument (Argument argument) {

    this.argument = argument;
  }
}
