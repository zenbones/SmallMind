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
package org.smallmind.web.json.doppelganger.translator;

import java.io.BufferedWriter;
import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

/**
 * Strategy interface for emitting the conversion expressions between an entity field and a generated view field.
 */
public interface Translator {

  /**
   * Writes the expression placed on the right-hand side of a view field assignment when constructing a view from an entity.
   *
   * @param writer                     destination for generated source
   * @param processingEnvironment      the current annotation processing environment
   * @param entityInstanceName         variable name of the source entity instance
   * @param entityFieldName            the logical field name on the entity
   * @param entityFieldTypeMirror      the type mirror of the entity field
   * @param viewFieldQualifiedTypeName the fully qualified view field type name
   * @throws IOException if writing to the source file fails
   */
  void writeRightSideOfEquals (BufferedWriter writer, ProcessingEnvironment processingEnvironment, String entityInstanceName, String entityFieldName, TypeMirror entityFieldTypeMirror, String viewFieldQualifiedTypeName)
    throws IOException;

  /**
   * Writes the expression placed inside a setter call that transfers a view field value back to the entity.
   *
   * @param writer                     destination for generated source
   * @param processingEnvironment      the current annotation processing environment
   * @param entityFieldTypeMirror      the type mirror of the entity field
   * @param viewFieldQualifiedTypeName the fully qualified view field type name
   * @param dtoFieldName               the name of the view field whose value is being transferred
   * @throws IOException if writing to the source file fails
   */
  void writeInsideOfSet (BufferedWriter writer, ProcessingEnvironment processingEnvironment, TypeMirror entityFieldTypeMirror, String viewFieldQualifiedTypeName, String dtoFieldName)
    throws IOException;
}
