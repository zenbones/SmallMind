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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link PropertyLexicon} real/virtual routing and the comment, as, and nullifier predicates.
 */
@Test(groups = "unit")
public class PropertyLexiconTest {

  private AnnotationMirror emptyMirror () {

    AnnotationMirror annotationMirror = Mockito.mock(AnnotationMirror.class);

    Mockito.when(annotationMirror.getElementValues()).thenReturn(Collections.EMPTY_MAP);

    return annotationMirror;
  }

  /**
   * Builds an annotation mirror that returns the given string for the {@code "comment"} element so that
   * the resulting {@link PropertyInformation} reports a non-empty comment.
   *
   * @param comment the comment text to surface through the mirror
   * @return a mirror whose only element value is the comment
   */
  private AnnotationMirror commentMirror (String comment) {

    AnnotationMirror annotationMirror = Mockito.mock(AnnotationMirror.class);
    ExecutableElement key = Mockito.mock(ExecutableElement.class);
    AnnotationValue value = Mockito.mock(AnnotationValue.class);
    Name name = Mockito.mock(Name.class);

    Mockito.when(name.contentEquals("comment")).thenReturn(true);
    Mockito.when(key.getSimpleName()).thenReturn(name);
    Mockito.when(value.getValue()).thenReturn(comment);
    Mockito.when(annotationMirror.getElementValues()).thenReturn((Map)Map.of(key, value));

    return annotationMirror;
  }

  private PropertyInformation property (AnnotationMirror annotationMirror, TypeMirror as, String nullifierMessage, boolean virtual) {

    return new PropertyInformation(annotationMirror, List.of(), false, null, nullifierMessage, virtual);
  }

  public void testRealAndVirtualRouting () {

    PropertyLexicon lexicon = new PropertyLexicon();

    Assert.assertFalse(lexicon.isReal());
    Assert.assertFalse(lexicon.isVirtual());

    lexicon.put("name", property(emptyMirror(), null, null, false));
    lexicon.put("token", property(emptyMirror(), null, null, true));

    Assert.assertTrue(lexicon.isReal());
    Assert.assertTrue(lexicon.isVirtual());
    Assert.assertTrue(lexicon.containsKey("name"));
    Assert.assertTrue(lexicon.containsKey("token"));
    Assert.assertFalse(lexicon.containsKey("missing"));
    Assert.assertEquals(lexicon.getRealMap().size(), 1);
    Assert.assertEquals(lexicon.getVirtualMap().size(), 1);
  }

  public void testHasCommentFalseWhenEmpty () {

    PropertyLexicon lexicon = new PropertyLexicon();

    lexicon.put("name", property(emptyMirror(), null, null, false));
    lexicon.put("token", property(emptyMirror(), null, null, true));

    Assert.assertFalse(lexicon.hasComment());
  }

  public void testHasCommentTrueForReal () {

    PropertyLexicon lexicon = new PropertyLexicon();

    lexicon.put("name", property(commentMirror("a comment"), null, null, false));

    Assert.assertTrue(lexicon.hasComment());
  }

  public void testHasCommentTrueForVirtual () {

    PropertyLexicon lexicon = new PropertyLexicon();

    lexicon.put("token", property(commentMirror("a comment"), null, null, true));

    Assert.assertTrue(lexicon.hasComment());
  }

  public void testHasNullifierOnlyConsidersReal () {

    PropertyLexicon realLexicon = new PropertyLexicon();
    PropertyLexicon virtualLexicon = new PropertyLexicon();

    realLexicon.put("name", property(emptyMirror(), null, "must be null", false));
    virtualLexicon.put("token", property(emptyMirror(), null, "must be null", true));

    Assert.assertTrue(realLexicon.hasNullifier());
    Assert.assertFalse(virtualLexicon.hasNullifier());
  }

  public void testHasNullifierFalseWhenNoMessage () {

    PropertyLexicon lexicon = new PropertyLexicon();

    lexicon.put("name", property(emptyMirror(), null, null, false));

    Assert.assertFalse(lexicon.hasNullifier());
  }

  public void testHasAsFalseWhenNoOverride () {

    PropertyLexicon lexicon = new PropertyLexicon();

    lexicon.put("name", property(emptyMirror(), null, null, false));

    Assert.assertFalse(lexicon.hasAs());
  }
}
