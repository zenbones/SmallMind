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
package org.smallmind.claxon.registry.spring;

import org.smallmind.claxon.registry.Tag;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs a singleton {@link Tag} from an injected
 * {@code key} and {@code value}. The {@link Tag} is assembled during
 * {@link #afterPropertiesSet()} after Spring has populated both properties. The underlying
 * {@link Tag} constructor enforces that neither key nor value may be {@code null} or empty.
 */
public class TagFactoryBean implements FactoryBean<Tag>, InitializingBean {

  /**
   * The {@link Tag} singleton produced by this factory bean.
   */
  private Tag tag;

  /**
   * The key component of the tag key/value pair.
   */
  private String key;

  /**
   * The value component of the tag key/value pair.
   */
  private String value;

  /**
   * Sets the key component of the tag.
   *
   * @param key the tag key; must be non-null and non-empty
   */
  public void setKey (String key) {

    this.key = key;
  }

  /**
   * Sets the value component of the tag.
   *
   * @param value the tag value; must be non-null and non-empty
   */
  public void setValue (String value) {

    this.value = value;
  }

  /**
   * Indicates that the produced {@link Tag} is a singleton within the Spring context.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory bean.
   *
   * @return {@link Tag}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return Tag.class;
  }

  /**
   * Returns the {@link Tag} singleton constructed during {@link #afterPropertiesSet()}.
   *
   * @return the constructed {@link Tag}, or {@code null} if {@link #afterPropertiesSet()} has
   * not yet run
   */
  @Override
  public Tag getObject () {

    return tag;
  }

  /**
   * Constructs the {@link Tag} from the configured {@link #key} and {@link #value}.
   * Invoked automatically by the Spring container after all bean properties have been set.
   *
   * @throws IllegalArgumentException if either {@link #key} or {@link #value} is {@code null} or empty
   */
  @Override
  public void afterPropertiesSet () {

    tag = new Tag(key, value);
  }
}
