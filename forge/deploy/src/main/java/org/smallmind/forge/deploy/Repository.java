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
package org.smallmind.forge.deploy;

/**
 * Nexus repository types understood by the deploy tooling. The code associated with each
 * constant corresponds to the repository identifier used in Nexus URL parameters.
 */
public enum Repository {

  /**
   * Nexus snapshots repository, used for development and integration builds.
   */
  SNAPSHOTS("snapshots"),
  /**
   * Nexus releases repository, used for stable, promoted artifacts.
   */
  RELEASES("releases");

  private final String code;

  /**
   * Initialise the enum constant with its Nexus URL code.
   *
   * @param code repository identifier as it appears in Nexus URL parameters
   */
  Repository (String code) {

    this.code = code;
  }

  /**
   * Look up a repository by its Nexus URL code.
   *
   * @param code repository identifier, e.g. {@code snapshots} or {@code releases}
   * @return the matching constant, or {@code null} if no constant carries that code
   */
  public static Repository fromCode (String code) {

    for (Repository repository : Repository.values()) {
      if (repository.getCode().equals(code)) {

        return repository;
      }
    }

    return null;
  }

  /**
   * Returns the repository identifier used in Nexus URL parameters.
   *
   * @return repository code, e.g. {@code releases} or {@code snapshots}
   */
  public String getCode () {

    return code;
  }
}
