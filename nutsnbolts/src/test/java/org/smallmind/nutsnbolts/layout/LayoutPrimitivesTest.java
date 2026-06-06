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
package org.smallmind.nutsnbolts.layout;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LayoutPrimitivesTest {

  public void testBiasEnumExposesBothAxes () {

    Assert.assertEquals(Bias.values().length, 2);
    Assert.assertSame(Bias.valueOf("HORIZONTAL"), Bias.HORIZONTAL);
    Assert.assertSame(Bias.valueOf("VERTICAL"), Bias.VERTICAL);
  }

  public void testJustificationEnumExposesFiveConstants () {

    Assert.assertEquals(Justification.values().length, 5);
    Assert.assertSame(Justification.valueOf("CENTER"), Justification.CENTER);
  }

  public void testAlignmentEnumExposesSixConstantsIncludingBaseline () {

    Assert.assertEquals(Alignment.values().length, 6);
    Assert.assertSame(Alignment.valueOf("BASELINE"), Alignment.BASELINE);
  }

  public void testFlowEnumExposesBothDirections () {

    Assert.assertEquals(Flow.values().length, 2);
    Assert.assertSame(Flow.valueOf("FIRST_TO_LAST"), Flow.FIRST_TO_LAST);
    Assert.assertSame(Flow.valueOf("LAST_TO_FIRST"), Flow.LAST_TO_FIRST);
  }

  public void testDimensionalityEnumExposesBothShapes () {

    Assert.assertEquals(Dimensionality.values().length, 2);
    Assert.assertSame(Dimensionality.valueOf("LINE"), Dimensionality.LINE);
    Assert.assertSame(Dimensionality.valueOf("PLANE"), Dimensionality.PLANE);
  }

  public void testOrientationDefaultIsHorizontalFirstToLast () {

    Orientation defaultOrientation = Orientation.getDefaultOrientation();

    Assert.assertSame(defaultOrientation.bias(), Bias.HORIZONTAL);
    Assert.assertSame(defaultOrientation.flow(), Flow.FIRST_TO_LAST);
  }

  public void testOrientationDefaultIsSharedInstance () {

    Assert.assertSame(Orientation.getDefaultOrientation(), Orientation.getDefaultOrientation());
  }

  public void testOrientationRecordCarriesProvidedValues () {

    Orientation custom = new Orientation(Bias.VERTICAL, Flow.LAST_TO_FIRST);

    Assert.assertSame(custom.bias(), Bias.VERTICAL);
    Assert.assertSame(custom.flow(), Flow.LAST_TO_FIRST);
  }

  public void testPairRecordExposesValues () {

    Pair pair = new Pair(3.0D, 4.0D);

    Assert.assertEquals(pair.first(), 3.0D);
    Assert.assertEquals(pair.second(), 4.0D);
    Assert.assertEquals(pair, new Pair(3.0D, 4.0D));
  }

  public void testPerimeterRecordExposesFourEdges () {

    Perimeter perimeter = new Perimeter(1.0D, 2.0D, 3.0D, 4.0D);

    Assert.assertEquals(perimeter.top(), 1.0D);
    Assert.assertEquals(perimeter.left(), 2.0D);
    Assert.assertEquals(perimeter.bottom(), 3.0D);
    Assert.assertEquals(perimeter.right(), 4.0D);
  }

  public void testPartialSolutionRecordExposesAllFields () {

    PartialSolution solution = new PartialSolution(Bias.VERTICAL, 10.0D, 25.0D);

    Assert.assertSame(solution.bias(), Bias.VERTICAL);
    Assert.assertEquals(solution.position(), 10.0D);
    Assert.assertEquals(solution.measurement(), 25.0D);
  }

  public void testSizingEqualityComparesAllThreeFields () {

    Object part = new Object();
    Sizing a = new Sizing(part, Bias.HORIZONTAL, TapeMeasure.PREFERRED);
    Sizing b = new Sizing(part, Bias.HORIZONTAL, TapeMeasure.PREFERRED);
    Sizing differentBias = new Sizing(part, Bias.VERTICAL, TapeMeasure.PREFERRED);
    Sizing differentMeasure = new Sizing(part, Bias.HORIZONTAL, TapeMeasure.MINIMUM);
    Sizing differentPart = new Sizing(new Object(), Bias.HORIZONTAL, TapeMeasure.PREFERRED);

    Assert.assertEquals(a, b);
    Assert.assertEquals(a.hashCode(), b.hashCode());
    Assert.assertNotEquals(a, differentBias);
    Assert.assertNotEquals(a, differentMeasure);
    Assert.assertNotEquals(a, differentPart);
    Assert.assertNotEquals(a, "not a sizing");
  }

  public void testGapNoneAlwaysZero () {

    Assert.assertEquals(Gap.NONE.getGap(new StubPlatform()), 0.0D);
  }

  public void testGapRelatedDelegatesToPlatform () {

    Assert.assertEquals(Gap.RELATED.getGap(new StubPlatform()), 4.0D);
  }

  public void testGapUnrelatedDelegatesToPlatform () {

    Assert.assertEquals(Gap.UNRELATED.getGap(new StubPlatform()), 8.0D);
  }

  private static final class StubPlatform implements ParaboxPlatform {

    @Override
    public double getRelatedGap () {

      return 4.0D;
    }

    @Override
    public double getUnrelatedGap () {

      return 8.0D;
    }

    @Override
    public Perimeter getFramePerimeter () {

      return new Perimeter(1.0D, 2.0D, 3.0D, 4.0D);
    }

    @Override
    public Orientation getOrientation () {

      return Orientation.getDefaultOrientation();
    }
  }
}
