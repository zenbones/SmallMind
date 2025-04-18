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
package org.smallmind.web.json.query;

import java.util.HashSet;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "where", namespace = "http://org.smallmind/web/json/query")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Where implements WherePermissible<Where> {

  private WhereConjunction rootConjunction;

  public Where () {

  }

  public Where (WhereConjunction rootConjunction) {

    this.rootConjunction = rootConjunction;
  }

  public static Where instance (WhereConjunction rootConjunction) {

    return new Where(rootConjunction);
  }

  @Override
  @XmlTransient
  public Set<WherePermit> getTargetSet ()
    throws Exception {

    HashSet<WherePermit> targetSet = new HashSet<>();

    WhereUtility.walk(new WhereVisitor() {

      @Override
      public void visitConjunction (WhereConjunction conjunction) {

      }

      @Override
      public void visitField (WhereField field) {

        targetSet.add(new TargetWherePermit(field.getEntity(), field.getName()));
      }
    }, this);

    return targetSet;
  }

  @XmlElement(name = "root")
  public WhereConjunction getRootConjunction () {

    return rootConjunction;
  }

  public void setRootConjunction (WhereConjunction rootConjunction) {

    this.rootConjunction = rootConjunction;
  }
}
