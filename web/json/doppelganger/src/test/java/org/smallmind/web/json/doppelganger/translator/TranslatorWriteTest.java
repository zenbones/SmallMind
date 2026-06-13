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
import java.io.StringWriter;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the writer emitting behavior of {@link NoopTranslator}, {@link ClassTranslator}, and the
 * setter side of {@link ListTranslator}, driven with a real {@link StringWriter} and a stub {@link TypeMirror}.
 */
@Test(groups = "unit")
public class TranslatorWriteTest {

  private TypeMirror typeMirror (TypeKind typeKind) {

    TypeMirror typeMirror = Mockito.mock(TypeMirror.class);

    Mockito.when(typeMirror.getKind()).thenReturn(typeKind);

    return typeMirror;
  }

  private String capture (Emitter emitter)
    throws IOException {

    StringWriter stringWriter = new StringWriter();

    try (BufferedWriter writer = new BufferedWriter(stringWriter)) {
      emitter.emit(writer);
    }

    return stringWriter.toString();
  }

  public void testNoopRightSideOfEqualsUsesGetter ()
    throws IOException {

    NoopTranslator translator = new NoopTranslator();
    String emitted = capture((writer) -> translator.writeRightSideOfEquals(writer, null, "widget", "name", typeMirror(TypeKind.DECLARED), "ignored"));

    Assert.assertEquals(emitted, "widget.getName();");
  }

  public void testNoopRightSideOfEqualsUsesIsForBoolean ()
    throws IOException {

    NoopTranslator translator = new NoopTranslator();
    String emitted = capture((writer) -> translator.writeRightSideOfEquals(writer, null, "widget", "active", typeMirror(TypeKind.BOOLEAN), "ignored"));

    Assert.assertEquals(emitted, "widget.isActive();");
  }

  public void testNoopInsideOfSetPassesThroughViewFieldName ()
    throws IOException {

    NoopTranslator translator = new NoopTranslator();
    String emitted = capture((writer) -> translator.writeInsideOfSet(writer, null, typeMirror(TypeKind.DECLARED), "ignored", "name"));

    Assert.assertEquals(emitted, "name");
  }

  public void testClassRightSideOfEqualsNullSafeInstance ()
    throws IOException {

    ClassTranslator translator = new ClassTranslator();
    String emitted = capture((writer) -> translator.writeRightSideOfEquals(writer, null, "widget", "child", typeMirror(TypeKind.DECLARED), "com.foo.ChildOutView"));

    Assert.assertEquals(emitted, "(widget.getChild() == null) ? null : com.foo.ChildOutView.instance(widget.getChild());");
  }

  public void testClassRightSideOfEqualsBooleanUsesIs ()
    throws IOException {

    ClassTranslator translator = new ClassTranslator();
    String emitted = capture((writer) -> translator.writeRightSideOfEquals(writer, null, "widget", "active", typeMirror(TypeKind.BOOLEAN), "com.foo.FlagOutView"));

    Assert.assertEquals(emitted, "(widget.isActive() == null) ? null : com.foo.FlagOutView.instance(widget.isActive());");
  }

  public void testClassInsideOfSetNullSafeFactory ()
    throws IOException {

    ClassTranslator translator = new ClassTranslator();
    String emitted = capture((writer) -> translator.writeInsideOfSet(writer, null, typeMirror(TypeKind.DECLARED), "com.foo.ChildInView", "child"));

    Assert.assertEquals(emitted, "(this.child == null) ? null : this.child.factory()");
  }

  public void testListInsideOfSetUsesMutator ()
    throws IOException {

    ListTranslator translator = new ListTranslator();
    String emitted = capture((writer) -> translator.writeInsideOfSet(writer, null, typeMirror(TypeKind.DECLARED), "java.util.List<com.foo.ChildInView>", "children"));

    Assert.assertEquals(emitted, "org.smallmind.web.json.scaffold.property.ListMutator.toEntityType(this.children)");
  }

  private interface Emitter {

    void emit (BufferedWriter writer)
      throws IOException;
  }
}
