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
package org.smallmind.web.json.query.jpa;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Minimal JPA entity fixture for {@link JPAQueryUtilityTest}, mapped against in-memory H2.
 */
@Entity
public class PersonEntity {

  @Id
  private Long id;
  private int age;
  private String status;
  private String name;
  private LocalDateTime created;

  public Long getId () {

    return id;
  }

  public void setId (Long id) {

    this.id = id;
  }

  public int getAge () {

    return age;
  }

  public void setAge (int age) {

    this.age = age;
  }

  public String getStatus () {

    return status;
  }

  public void setStatus (String status) {

    this.status = status;
  }

  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  public LocalDateTime getCreated () {

    return created;
  }

  public void setCreated (LocalDateTime created) {

    this.created = created;
  }
}
