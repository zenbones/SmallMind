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
package org.smallmind.spark.singularity.boot;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ModuleFinder} over the modules recorded in a Singularity bundle's index, used to resolve the configuration of
 * the module layer that {@link SingularityEntryPoint} defines for a bundle built in modular mode.
 */
public class SingularityModuleFinder implements ModuleFinder {

  private final HashMap<String, ModuleReference> moduleReferenceMap = new HashMap<>();

  /**
   * @param singularityModules the modules this finder can locate
   */
  public SingularityModuleFinder (Collection<SingularityModule> singularityModules) {

    for (SingularityModule singularityModule : singularityModules) {
      moduleReferenceMap.put(singularityModule.getName(), singularityModule.createModuleReference());
    }
  }

  @Override
  public Optional<ModuleReference> find (String name) {

    return Optional.ofNullable(moduleReferenceMap.get(name));
  }

  @Override
  public Set<ModuleReference> findAll () {

    return new HashSet<>(moduleReferenceMap.values());
  }
}
