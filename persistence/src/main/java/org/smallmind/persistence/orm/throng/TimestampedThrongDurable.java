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
package org.smallmind.persistence.orm.throng;

import java.io.Serializable;
import java.util.Date;
import org.smallmind.mongodb.throng.index.IndexType;
import org.smallmind.mongodb.throng.index.annotation.Indexed;
import org.smallmind.mongodb.throng.lifecycle.annotation.PrePersist;
import org.smallmind.mongodb.throng.mapping.annotation.Property;

/**
 * Throng durable that tracks creation and update timestamps and maintains an index on lastUpdated.
 *
 * @param <I> identifier type
 * @param <D> durable subclass type
 */
public abstract class TimestampedThrongDurable<I extends Serializable & Comparable<I>, D extends TimestampedThrongDurable<I, D>> extends ThrongDurable<I, D> {

  @Property("created")
  private Date created;
  @Property("lastUpdated")
  @Indexed(IndexType.DESCENDING)
  private Date lastUpdated;

  /**
   * @return the creation timestamp
   */
  public Date getCreated () {

    return created;
  }

  /**
   * Sets the creation timestamp.
   *
   * @param created timestamp
   */
  public void setCreated (Date created) {

    this.created = created;
  }

  /**
   * @return the last update timestamp
   */
  public Date getLastUpdated () {

    return lastUpdated;
  }

  /**
   * Sets the last update timestamp.
   *
   * @param lastUpdated timestamp
   */
  public void setLastUpdated (Date lastUpdated) {

    this.lastUpdated = lastUpdated;
  }

  /**
   * Populates timestamps prior to persistence.
   */
  @PrePersist
  public void prePersist () {

    if (created == null) {
      created = new Date();
    }

    lastUpdated = new Date();
  }
}
