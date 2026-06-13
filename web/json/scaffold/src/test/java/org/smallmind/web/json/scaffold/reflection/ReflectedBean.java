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
package org.smallmind.web.json.scaffold.reflection;

/**
 * Public top-level bean fixture for {@link BeanReflectorBranchTest} exercising a mix of read-only,
 * write-only, {@code is}-boolean, plain field, array, and method-invocation accessors.
 */
public class ReflectedBean extends ReflectedBeanParent {

  public String publicField;
  private int[] cells;
  private int[][] grid;
  private boolean enabled;
  private int writeOnly;
  private String label;

  public boolean isEnabled () {

    return enabled;
  }

  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  // Read-only property: getter without a matching setter.
  public String getReadOnly () {

    return "fixed";
  }

  // Write-only property: setter without a matching getter.
  public void setWriteOnly (int writeOnly) {

    this.writeOnly = writeOnly;
  }

  public int getWriteOnly () {

    return writeOnly;
  }

  public String getLabel () {

    return label;
  }

  public void setLabel (String label) {

    this.label = label;
  }

  public int[] getCells () {

    return cells;
  }

  public void setCells (int[] cells) {

    this.cells = cells;
  }

  public int[][] getGrid () {

    return grid;
  }

  public void setGrid (int[][] grid) {

    this.grid = grid;
  }

  // An 'is' prefixed accessor that does not return a boolean type.
  public String isMislabeled () {

    return "not-a-boolean";
  }

  public String echo (String who) {

    return "echo:" + who;
  }

  public String ping () {

    return "pong";
  }
}
