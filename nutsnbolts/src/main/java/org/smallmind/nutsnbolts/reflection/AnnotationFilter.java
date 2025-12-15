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
package org.smallmind.nutsnbolts.reflection;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Filters annotation descriptors as they are encountered during byte code parsing.
 * The filter can either include only a configured set of annotations or exclude them,
 * depending on the configured {@link PassType}.
 */
public class AnnotationFilter {

  private final PassType passType;
  private String[] annotationSignatures = null;

  /**
   * Constructs a filter that either allows only the supplied annotations or blocks them.
   *
   * @param passType          determines whether the supplied annotations are included or excluded
   * @param annotationClasses annotation classes expressed as {@link Class} objects; when omitted, all descriptors pass
   */
  public AnnotationFilter (PassType passType, Class... annotationClasses) {

    this.passType = passType;

    if (annotationClasses != null) {
      annotationSignatures = new String[annotationClasses.length];
      for (int index = 0; index < annotationSignatures.length; index++) {
        annotationSignatures[index] = "L" + annotationClasses[index].getName().replace('.', '/') + ";";
      }
    }
  }

  /**
   * Tests whether the supplied annotation descriptor should be allowed based on the configured mode.
   *
   * @param desc the JVM descriptor of the annotation (for example {@code Ljava/lang/Deprecated;})
   * @return {@code true} if the descriptor is allowed for the current mode
   * @throws UnknownSwitchCaseException if the configured {@link PassType} is not recognized
   */
  public boolean isAllowed (String desc) {

    switch (passType) {
      case INCLUDE:
        if (annotationSignatures != null) {
          for (String annotationSignature : annotationSignatures) {
            if (annotationSignature.equals(desc)) {

              return true;
            }
          }
        }

        return false;
      case EXCLUDE:
        if (annotationSignatures != null) {
          for (String annotationSignature : annotationSignatures) {
            if (annotationSignature.equals(desc)) {

              return false;
            }
          }
        }

        return true;
      default:
        throw new UnknownSwitchCaseException(passType.name());
    }
  }
}
