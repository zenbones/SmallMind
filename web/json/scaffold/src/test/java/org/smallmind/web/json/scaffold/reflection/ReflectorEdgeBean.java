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
 * Public top-level bean fixture for {@link BeanReflectorEdgeTest} that exercises the reflector's
 * exception and deeper-traversal branches: getters, setters, and methods that throw at invocation
 * time, a mid-chain method call that returns {@code this}, a public array field reached through a
 * subscript, a {@code String[]} property used to drive the non-array penultimate failure, an array
 * holding a {@code null} element to drive the null penultimate failure, and a three-dimensional
 * array to drive the higher ordinal labels of {@code indexToNth}.
 */
public class ReflectorEdgeBean {

  public int[] publicCells;
  private final String label = "edge";
  private String[] words;
  private int[] holes;
  private int[][][] cube;

  // A getter that always fails at invocation time, surfacing as an InvocationTargetException.
  public String getBoom () {

    throw new IllegalStateException("getter boom");
  }

  // A setter that always fails at invocation time, surfacing as an InvocationTargetException.
  public void setBoom (String boom) {

    throw new IllegalStateException("setter boom");
  }

  // A method that always fails at invocation time, surfacing as an InvocationTargetException.
  public String detonate () {

    throw new IllegalStateException("method boom");
  }

  // A readable array property whose setter always throws, so a subscripted set reaches the array
  // mutation through the getter and then fails when the setter method itself is invoked.
  public int[] getBoomCells () {

    return new int[] {1, 2, 3};
  }

  public void setBoomCells (int[] boomCells) {

    throw new IllegalStateException("setter boom");
  }

  // A single-argument method whose parameter type can not be produced from an arbitrary argument.
  public String needsNumber (int value) {

    return "number:" + value;
  }

  // Returns the same bean so a mid-chain method call can be followed by a further getter.
  public ReflectorEdgeBean self () {

    return this;
  }

  public String getLabel () {

    return label;
  }

  public String[] getWords () {

    return words;
  }

  public void setWords (String[] words) {

    this.words = words;
  }

  public int[] getHoles () {

    return holes;
  }

  public void setHoles (int[] holes) {

    this.holes = holes;
  }

  public int[][][] getCube () {

    return cube;
  }

  public void setCube (int[][][] cube) {

    this.cube = cube;
  }
}
