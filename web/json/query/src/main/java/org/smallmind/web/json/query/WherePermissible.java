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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Describes an object (typically a {@link Where}) that can expose the set of field permits it references
 * and validate them against allowed/required/excluded/dependent constraints.
 *
 * @param <W> self type for fluent validation
 */
public interface WherePermissible<W extends WherePermissible<W>> {

  /**
   * Returns the set of target permits referenced by this instance.
   *
   * @return set of permits
   * @throws Exception if traversal fails
   */
  Set<WherePermit> getTargetSet ()
    throws Exception;

  /**
   * Validates this instance against the provided permit rules.
   *
   * @param permits permit rules to enforce
   * @return {@code this} for chaining
   * @throws WhereValidationException   if any rule is violated (missing required, present excluded, or failed dependency)
   * @throws UnknownSwitchCaseException if an unexpected permit type is encountered
   * @throws Exception                  if computing target permits fails
   */
  default W validate (WherePermit... permits)
    throws Exception {

    if ((permits != null) && (permits.length > 0)) {

      Set<WherePermit> requestedSet = getTargetSet();
      HashSet<WherePermit> allowedSet = new HashSet<>();
      HashSet<WherePermit> requiredSet = new HashSet<>();
      HashSet<WherePermit> excludedSet = new HashSet<>();
      HashSet<WherePermit> failedDependencySet = new HashSet<>();

      for (WherePermit permit : permits) {
        switch (permit.getType()) {
          case ALLOWED -> allowedSet.add(permit);
          case REQUIRED -> {
            allowedSet.add(permit);
            requiredSet.add(permit);
          }
          case EXCLUDED -> excludedSet.add(permit);
          case DEPENDENT -> {
            if (requestedSet.contains(permit) && (!requestedSet.contains(((DependentWherePermit)permit).getRequirement()))) {
              failedDependencySet.add(permit);
            }
          }
        }
      }

      if (!failedDependencySet.isEmpty()) {
        throw new WhereValidationException("The elements(%s) are failed dependencies for this query", Arrays.toString(failedDependencySet.toArray()), this.getClass().getSimpleName());
      }
      if (!requestedSet.containsAll(requiredSet)) {
        throw new WhereValidationException("The elements(%s) are required in %s clauses for this query", Arrays.toString(requiredSet.toArray()), this.getClass().getSimpleName());
      }
      for (WherePermit target : requestedSet) {
        if (excludedSet.contains(target)) {
          throw new WhereValidationException("The element(%s) is not permitted in %s clauses for this query", target, this.getClass().getSimpleName());
        }
        if ((!allowedSet.isEmpty()) && (!allowedSet.contains(target))) {
          throw new WhereValidationException("The element(%s) is not permitted in %s clauses for this query", target, this.getClass().getSimpleName());
        }
      }
    }

    return (W)this;
  }
}
