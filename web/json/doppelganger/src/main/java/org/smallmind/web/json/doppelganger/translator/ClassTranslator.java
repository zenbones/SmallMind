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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;

/**
 * Translator for class-typed properties whose type has a generated view representation.
 */
public class ClassTranslator implements Translator {

  /**
   * Emits a null-safe expression that calls the view's {@code instance()} factory from the entity getter.
   *
   * @param writer                     destination for generated source
   * @param processingEnvironment      the current annotation processing environment
   * @param entityInstanceName         variable name of the source entity instance
   * @param entityFieldName            the logical field name on the entity
   * @param entityFieldTypeMirror      the type mirror of the entity field
   * @param viewFieldQualifiedTypeName the fully qualified view field type name
   * @throws IOException if writing to the source file fails
   */
  @Override
  public void writeRightSideOfEquals (BufferedWriter writer, ProcessingEnvironment processingEnvironment, String entityInstanceName, String entityFieldName, TypeMirror entityFieldTypeMirror, String viewFieldQualifiedTypeName)
    throws IOException {

    writer.write("(");
    writer.write(entityInstanceName);
    writer.write(".");
    writer.write(TypeKind.BOOLEAN.equals(entityFieldTypeMirror.getKind()) ? BeanUtility.asIsName(entityFieldName) : BeanUtility.asGetterName(entityFieldName));
    writer.write("() == null) ? null : ");
    writer.write(viewFieldQualifiedTypeName);
    writer.write(".instance(");
    writer.write(entityInstanceName);
    writer.write(".");
    writer.write(TypeKind.BOOLEAN.equals(entityFieldTypeMirror.getKind()) ? BeanUtility.asIsName(entityFieldName) : BeanUtility.asGetterName(entityFieldName));
    writer.write("());");
  }

  /**
   * Emits a null-safe expression that calls the view's {@code factory()} method to reconstruct the entity value.
   *
   * @param writer                     destination for generated source
   * @param processingEnvironment      the current annotation processing environment
   * @param entityFieldTypeMirror      the type mirror of the entity field
   * @param viewFieldQualifiedTypeName the fully qualified view field type name
   * @param viewFieldName              the name of the view field
   * @throws IOException if writing to the source file fails
   */
  @Override
  public void writeInsideOfSet (BufferedWriter writer, ProcessingEnvironment processingEnvironment, TypeMirror entityFieldTypeMirror, String viewFieldQualifiedTypeName, String viewFieldName)
    throws IOException {

    writer.write("(this.");
    writer.write(viewFieldName);
    writer.write(" == null) ? null : this.");
    writer.write(viewFieldName);
    writer.write(".factory()");
  }
}
