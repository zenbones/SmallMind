/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.web.json.scaffold.property.ListMutator;
import org.smallmind.web.json.scaffold.property.PropertyException;

/**
 * Translator for {@link java.util.List} properties, delegating element conversion to {@link ListMutator}.
 */
public class ListTranslator implements Translator {

  private static final String LIST_MUTATOR_NAME = ListMutator.class.getCanonicalName();

  /**
   * Emits code to convert an entity list into a view list using {@link ListMutator#toViewType(Class, Class, java.util.List)}.
   */
  @Override
  public void writeRightSideOfEquals (BufferedWriter writer, ProcessingEnvironment processingEnvironment, String entityInstanceName, String entityFieldName, TypeMirror entityFieldTypeMirror, String viewFieldQualifiedTypeName)
    throws IOException {

    writer.write(LIST_MUTATOR_NAME);
    writer.write(".toViewType(");
    writer.write(((TypeElement)processingEnvironment.getTypeUtils().asElement(((DeclaredType)entityFieldTypeMirror).getTypeArguments().get(0))).getQualifiedName().toString());
    writer.write(".class, ");
    writer.write(extractViewTypeName(viewFieldQualifiedTypeName));
    writer.write(".class, ");
    writer.write(entityInstanceName);
    writer.write(".");
    writer.write(TypeKind.BOOLEAN.equals(entityFieldTypeMirror.getKind()) ? BeanUtility.asIsName(entityFieldName) : BeanUtility.asGetterName(entityFieldName));
    writer.write("());");
  }

  /**
   * Emits code to convert a view list back into an entity list.
   */
  @Override
  public void writeInsideOfSet (BufferedWriter writer, ProcessingEnvironment processingEnvironment, TypeMirror entityFieldTypeMirror, String viewFieldQualifiedTypeName, String viewFieldName)
    throws IOException {

    writer.write(LIST_MUTATOR_NAME);
    writer.write(".toEntityType(this.");
    writer.write(viewFieldName);
    writer.write(")");
  }

  /**
   * Extracts the element type name from a parameterized list type string (e.g., {@code java.util.List<com.foo.Bar>}).
   *
   * @param qualifiedListTypeName fully qualified parameterized list name
   * @return the element type name
   * @throws PropertyException if the name cannot be parsed
   */
  private String extractViewTypeName (String qualifiedListTypeName) {

    int leftAnglePos;

    if ((leftAnglePos = qualifiedListTypeName.indexOf('<')) >= 0) {

      int rightAnglePos;

      if ((rightAnglePos = qualifiedListTypeName.indexOf('>', leftAnglePos + 1)) >= 0) {

        return qualifiedListTypeName.substring(leftAnglePos + 1, rightAnglePos);
      }
    }

    throw new PropertyException("Could not extract view type from '%s'", qualifiedListTypeName);
  }
}
