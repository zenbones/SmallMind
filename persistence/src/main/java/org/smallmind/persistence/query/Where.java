/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.query;

import java.io.Serializable;
import java.util.HashSet;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "where")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Where implements Serializable {

  private WhereConjunction rootConjunction;

  public Where () {

  }

  public Where (WhereConjunction rootConjunction) {

    this.rootConjunction = rootConjunction;
  }

  public void validate (WherePermit... permits) {

    final HashSet<String> fieldNameSet = new HashSet<>();

    WhereUtility.walk(this, new WhereVisitor() {

      @Override
      public void visitConjunction (WhereConjunction conjunction) {

      }

      @Override
      public void visitField (WhereField field) {

        fieldNameSet.add(field.getName());
      }
    });

    if ((permits != null) && (permits.length > 0)) {
      for (WherePermit permit : permits) {
        if ((permit.getFields() != null) && (permit.getFields().length > 0)) {
          permit.getType().validate(fieldNameSet, permit.getFields());
        }
      }
    }
  }

  @XmlElement(name = "root")
  @XmlElementRefs({@XmlElementRef(type = AndWhereConjunction.class), @XmlElementRef(type = OrWhereConjunction.class)})
  public WhereConjunction getRootConjunction () {

    return rootConjunction;
  }

  public void setRootConjunction (WhereConjunction rootConjunction) {

    this.rootConjunction = rootConjunction;
  }
}