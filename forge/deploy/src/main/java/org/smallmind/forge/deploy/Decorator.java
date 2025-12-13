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

import java.nio.file.Path;

/**
 * Hook for performing platform or environment specific updates after an application is deployed.
 * Implementations may create service wrappers, configuration files, or any other installation side-effects.
 */
public interface Decorator {

  /**
   * Apply a decoration step to the extracted installation.
   *
   * @param operatingSystem the target operating system
   * @param appUser         the system user that should own the installation
   * @param installPath     the root installation directory
   * @param nexusHost       the Nexus host that supplied the artifact
   * @param nexusUser       the Nexus user
   * @param nexusPassword   the Nexus password
   * @param repository      the repository from which the artifact was retrieved
   * @param groupId         the artifact group id
   * @param artifactId      the artifact id
   * @param version         the artifact version
   * @param classifier      the artifact classifier, or {@code null}
   * @param extension       the artifact extension (e.g. {@code jar})
   * @param envVars         optional environment variable declarations to apply
   * @throws Exception if any decoration step fails; implementations may throw IO or templating exceptions
   */
  void decorate (OperatingSystem operatingSystem, String appUser, Path installPath, String nexusHost, String nexusUser, String nexusPassword, Repository repository, String groupId, String artifactId, String version, String classifier, String extension, String... envVars)
    throws Exception;
}
