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
package org.smallmind.web.json.query;

import java.util.HashSet;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Container for a set of sort fields expressed alongside where clauses.
 */
@XmlRootElement(name = "sort", namespace = "http://org.smallmind/web/json/query")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Sort implements WherePermissible<Sort> {

  private SortField[] fields;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public Sort () {

  }

  /**
   * Creates a sort definition with the provided fields.
   *
   * @param fields sort fields
   */
  public Sort (SortField... fields) {

    this.fields = fields;
  }

  /**
   * Convenience factory for a sort definition.
   *
   * @param fields sort fields
   * @return sort container
   */
  public static Sort instance (SortField... fields) {

    return new Sort(fields);
  }

  /**
   * Exposes the set of target permits for validation (each sorted field).
   *
   * @return targets derived from sort fields
   */
  @Override
  @XmlTransient
  public Set<WherePermit> getTargetSet () {

    HashSet<WherePermit> targetSet = new HashSet<>();

    for (SortField field : fields) {
      targetSet.add(new TargetWherePermit(field.getEntity(), field.getName()));
    }

    return targetSet;
  }

  /**
   * Indicates whether the sort definition contains any fields.
   *
   * @return {@code true} if no sort fields were specified
   */
  @XmlTransient
  public synchronized boolean isEmpty () {

    return (fields == null) || (fields.length == 0);
  }

  /**
   * Returns the sort fields.
   *
   * @return array of sort fields or {@code null}
   */
  @XmlElement(name = "fields")
  public synchronized SortField[] getFields () {

    return fields;
  }

  /**
   * Sets the sort fields.
   *
   * @param fields sort fields
   */
  public synchronized void setFields (SortField... fields) {

    this.fields = fields;
  }
}
