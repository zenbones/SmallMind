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
package org.smallmind.sleuth.runner.annotation;

import org.smallmind.nutsnbolts.lang.AnnotationLiteral;

public class SuiteLiteral extends AnnotationLiteral<Suite> implements Suite {

  private final String[] dependsOn;
  private final boolean enabled;
  private final int priority;
  private String[] executeAfter;
  private String[] groups;

  public SuiteLiteral () {

    priority = 0;
    dependsOn = new String[0];
    enabled = true;
  }

  public SuiteLiteral (String[] groups, int priority, String[] executeAfter, String[] dependsOn, boolean enabled) {

    this.groups = groups;
    this.priority = priority;
    this.executeAfter = executeAfter;
    this.dependsOn = dependsOn;
    this.enabled = enabled;
  }

  @Override
  public String[] groups () {

    return groups;
  }

  @Override
  public int priority () {

    return priority;
  }

  @Override
  public String[] executeAfter () {

    return executeAfter;
  }

  @Override
  public String[] dependsOn () {

    return dependsOn;
  }

  @Override
  public boolean enabled () {

    return enabled;
  }
}
