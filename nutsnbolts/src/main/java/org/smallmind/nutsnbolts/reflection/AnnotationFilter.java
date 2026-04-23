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
 * Determines whether a given annotation descriptor should be copied to a generated proxy class,
 * using an {@link PassType#INCLUDE} or {@link PassType#EXCLUDE} policy against a configured set of annotations.
 */
public class AnnotationFilter {

  private final PassType passType;
  private String[] annotationSignatures = null;

  /**
   * Constructs a filter with the specified policy and annotation set.
   *
   * @param passType          {@link PassType#INCLUDE} to allow only the listed annotations, or
   *                          {@link PassType#EXCLUDE} to block only the listed annotations
   * @param annotationClasses the annotation types the policy applies to; when absent all descriptors
   *                          are allowed ({@code EXCLUDE}) or none are ({@code INCLUDE})
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
   * Decides whether the given annotation descriptor passes the configured include/exclude policy.
   *
   * @param desc the JVM type descriptor of the annotation, e.g. {@code Ljava/lang/Deprecated;}
   * @return {@code true} if the annotation should be kept according to the policy; {@code false} if it should be dropped
   * @throws UnknownSwitchCaseException if the configured {@link PassType} is not handled by this method
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
