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
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the small parsed-annotation value holders, exercising their default-value behavior when
 * the backing annotation mirror carries no explicitly set elements.
 */
@Test(groups = "unit")
public class InformationValueTest {

  private AnnotationMirror emptyMirror () {

    AnnotationMirror annotationMirror = Mockito.mock(AnnotationMirror.class);

    Mockito.when(annotationMirror.getElementValues()).thenReturn(Collections.EMPTY_MAP);

    return annotationMirror;
  }

  public void testConstraintInformationDefaults () {

    ConstraintInformation constraintInformation = new ConstraintInformation(emptyMirror());

    Assert.assertNull(constraintInformation.getType());
    Assert.assertEquals(constraintInformation.getArguments(), "");
  }

  public void testPledgeInformationDefaults () {

    PledgeInformation pledgeInformation = new PledgeInformation(emptyMirror());

    Assert.assertEquals(pledgeInformation.getVisibility(), Visibility.BOTH);
    Assert.assertTrue(pledgeInformation.getPurposeList().isEmpty());
  }

  public void testPolymorphicInformationDefaults () {

    ProcessingEnvironment processingEnvironment = Mockito.mock(ProcessingEnvironment.class);
    PolymorphicInformation polymorphicInformation = new PolymorphicInformation(processingEnvironment, emptyMirror());

    Assert.assertFalse(polymorphicInformation.isUseAttribute());
    Assert.assertTrue(polymorphicInformation.getSubClassList().isEmpty());
  }

  public void testHierarchyInformationDefaults () {

    ProcessingEnvironment processingEnvironment = Mockito.mock(ProcessingEnvironment.class);
    HierarchyInformation hierarchyInformation = new HierarchyInformation(processingEnvironment, emptyMirror());

    Assert.assertTrue(hierarchyInformation.getSubClassList().isEmpty());
  }

  public void testPropertyInformationDefaultsAndIdiomRequired () {

    PropertyInformation defaultInformation = new PropertyInformation(emptyMirror(), java.util.List.of(), false, null, null, false);

    Assert.assertEquals(defaultInformation.getName(), "");
    Assert.assertEquals(defaultInformation.getComment(), "");
    Assert.assertNull(defaultInformation.getAdapter());
    Assert.assertNull(defaultInformation.getAs());
    Assert.assertNull(defaultInformation.getNullifierMessage());
    Assert.assertFalse(defaultInformation.isVirtual());
    Assert.assertFalse(defaultInformation.isRequired());

    PropertyInformation requiredInformation = new PropertyInformation(emptyMirror(), java.util.List.of(), true, null, null, true);

    Assert.assertTrue(requiredInformation.isVirtual());
    Assert.assertTrue(requiredInformation.isRequired());
  }
}
