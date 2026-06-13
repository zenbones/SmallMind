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
package org.smallmind.web.json.doppelganger;

import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.element.TypeElement;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link NameUtility} view-name composition, package lookup, and primitive type rendering.
 */
@Test(groups = "unit")
public class NameUtilityTest {

  private Name name (String value) {

    return new StringName(value);
  }

  private ProcessingEnvironment environmentWithPrefix (String prefix) {

    ProcessingEnvironment processingEnvironment = Mockito.mock(ProcessingEnvironment.class);

    Mockito.when(processingEnvironment.getOptions()).thenReturn((prefix == null) ? Map.of() : Map.of("prefix", prefix));

    return processingEnvironment;
  }

  public void testGetSimpleNameNoPurposeNoPrefix () {

    TypeElement typeElement = Mockito.mock(TypeElement.class);

    Mockito.when(typeElement.getSimpleName()).thenReturn(name("Widget"));

    Assert.assertEquals(NameUtility.getSimpleName(environmentWithPrefix(null), "", Direction.IN, typeElement), "WidgetInView");
    Assert.assertEquals(NameUtility.getSimpleName(environmentWithPrefix(null), null, Direction.OUT, typeElement), "WidgetOutView");
  }

  public void testGetSimpleNameWithPurpose () {

    TypeElement typeElement = Mockito.mock(TypeElement.class);

    Mockito.when(typeElement.getSimpleName()).thenReturn(name("Widget"));

    Assert.assertEquals(NameUtility.getSimpleName(environmentWithPrefix(null), "create", Direction.IN, typeElement), "WidgetCreateInView");
  }

  public void testGetSimpleNameWithPrefix () {

    TypeElement typeElement = Mockito.mock(TypeElement.class);

    Mockito.when(typeElement.getSimpleName()).thenReturn(name("Widget"));

    Assert.assertEquals(NameUtility.getSimpleName(environmentWithPrefix("Gen"), "", Direction.OUT, typeElement), "GenWidgetOutView");
  }

  public void testGetPackageName () {

    ProcessingEnvironment processingEnvironment = Mockito.mock(ProcessingEnvironment.class);
    Elements elements = Mockito.mock(Elements.class);
    PackageElement packageElement = Mockito.mock(PackageElement.class);
    TypeElement typeElement = Mockito.mock(TypeElement.class);

    Mockito.when(packageElement.getQualifiedName()).thenReturn(name("com.foo"));
    Mockito.when(elements.getPackageOf(typeElement)).thenReturn(packageElement);
    Mockito.when(processingEnvironment.getElementUtils()).thenReturn(elements);

    Assert.assertEquals(NameUtility.getPackageName(processingEnvironment, typeElement), "com.foo");
  }

  public void testProcessTypeMirrorPrimitiveFallsThroughToToString () {

    TypeMirror typeMirror = Mockito.mock(TypeMirror.class);

    Mockito.when(typeMirror.getKind()).thenReturn(TypeKind.INT);
    Mockito.when(typeMirror.toString()).thenReturn("int");

    Assert.assertEquals(NameUtility.processTypeMirror(null, null, null, "", Direction.IN, typeMirror), "int");
  }

  public void testProcessTypeMirrorTypeVarRendersWildcard () {

    TypeMirror typeMirror = Mockito.mock(TypeMirror.class);

    Mockito.when(typeMirror.getKind()).thenReturn(TypeKind.TYPEVAR);

    Assert.assertEquals(NameUtility.processTypeMirror(null, null, null, "", Direction.IN, typeMirror), "?");
  }

  private static final class StringName implements Name {

    private final String value;

    private StringName (String value) {

      this.value = value;
    }

    @Override
    public boolean contentEquals (CharSequence cs) {

      return value.contentEquals(cs);
    }

    @Override
    public int length () {

      return value.length();
    }

    @Override
    public char charAt (int index) {

      return value.charAt(index);
    }

    @Override
    public CharSequence subSequence (int start, int end) {

      return value.subSequence(start, end);
    }

    @Override
    public String toString () {

      return value;
    }
  }
}
