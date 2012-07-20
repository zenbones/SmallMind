/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.aop;

import org.smallmind.nutsnbolts.lang.AnnotationLiteral;
import org.smallmind.persistence.Durable;

public class KeyLiteral extends AnnotationLiteral<Key> implements Key {

  private Class<? extends Durable> with;
  private String on;
  private String constant;
  private String alias;
  private boolean nullable;

  public KeyLiteral (Class<? extends Durable> with, String on) {

    this(with, on, "", "", false);
  }

  public KeyLiteral (Class<? extends Durable> with, String on, String alias) {

    this(with, on, alias, "", false);
  }

  public KeyLiteral (Class<? extends Durable> with, String on, String alias, String constant) {

    this(with, on, alias, constant, false);
  }

  public KeyLiteral (Class<? extends Durable> with, String on, String alias, String constant, boolean nullable) {

    this.with = with;
    this.on = on;
    this.alias = alias;
    this.constant = constant;
    this.nullable = nullable;
  }

  @Override
  public Class<? extends Durable> with () {

    return with;
  }

  @Override
  public String on () {

    return on;
  }

  @Override
  public String constant () {

    return constant;
  }

  @Override
  public String alias () {

    return alias;
  }

  @Override
  public boolean nullable () {

    return nullable;
  }
}
