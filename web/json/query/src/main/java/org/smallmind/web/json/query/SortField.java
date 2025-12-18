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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Describes a single sort field, including optional entity alias and direction.
 */
@XmlRootElement(name = "field", namespace = "http://org.smallmind/web/json/query")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class SortField {

  private SortDirection direction;
  private String entity;
  private String name;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public SortField () {

  }

  /**
   * Creates a sort field in the default entity with the given name and direction.
   *
   * @param name      field name
   * @param direction sort direction
   */
  public SortField (String name, SortDirection direction) {

    this.name = name;
    this.direction = direction;
  }

  /**
   * Creates a sort field scoped to an entity with the given name and direction.
   *
   * @param entity    entity alias
   * @param name      field name
   * @param direction sort direction
   */
  public SortField (String entity, String name, SortDirection direction) {

    this(name, direction);

    this.entity = entity;
  }

  /**
   * Convenience factory for a sort field in the default entity.
   *
   * @param name      field name
   * @param direction sort direction
   * @return new sort field
   */
  public static SortField instance (String name, SortDirection direction) {

    return new SortField(name, direction);
  }

  /**
   * Convenience factory for a sort field scoped to an entity.
   *
   * @param entity    entity alias
   * @param name      field name
   * @param direction sort direction
   * @return new sort field
   */
  public static SortField instance (String entity, String name, SortDirection direction) {

    return new SortField(entity, name, direction);
  }

  /**
   * @return entity alias or {@code null} for default entity
   */
  @XmlElement(name = "entity")
  public String getEntity () {

    return entity;
  }

  /**
   * Sets the entity alias.
   *
   * @param entity entity alias
   */
  public void setEntity (String entity) {

    this.entity = entity;
  }

  /**
   * @return field name
   */
  @XmlElement(name = "name", required = true)
  public String getName () {

    return name;
  }

  /**
   * Sets the field name.
   *
   * @param name field name
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * @return sort direction
   */
  @XmlElement(name = "direction", required = true)
  @XmlJavaTypeAdapter(SortDirectionEnumXmlAdapter.class)
  public SortDirection getDirection () {

    return direction;
  }

  /**
   * Sets the sort direction.
   *
   * @param direction sort direction
   */
  public void setDirection (SortDirection direction) {

    this.direction = direction;
  }

  /**
   * Hashes by field name for use in sets/maps.
   *
   * @return hash code
   */
  @Override
  public int hashCode () {

    return name.hashCode();
  }

  /**
   * Equality based on field name (entity-agnostic).
   *
   * @param obj other object
   * @return {@code true} if the other sort field shares the same name
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof SortField) && ((SortField)obj).getName().equals(name);
  }
}
