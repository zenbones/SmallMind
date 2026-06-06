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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ParaboxLayoutEngineTest {

  public void testPreferredSizeReflectsComponentRequests () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox().add(left).add(right));

    Assert.assertEquals(layout.calculatePreferredWidth(), 70.0D);
    Assert.assertEquals(layout.calculatePreferredHeight(), 25.0D);

    Pair preferredSize = layout.calculatePreferredSize();
    Assert.assertEquals(preferredSize.first(), 70.0D);
    Assert.assertEquals(preferredSize.second(), 25.0D);
  }

  public void testMinimumSizeReflectsMinimumPlusUnshrinkingMembers () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left, Constraint.stretch()).add(right, Constraint.stretch()));
    layout.setVerticalBox(layout.parallelBox().add(left).add(right));

    Pair minimum = layout.calculateMinimumSize();
    Assert.assertEquals(minimum.first(), 20.0D);
    Assert.assertEquals(minimum.second(), 25.0D);
  }

  public void testDoLayoutPlacesComponentsAtExpectedCoordinates () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(200, 100, Arrays.asList(left, right));

    Assert.assertNotNull(left.getLocation(), "left was not placed");
    Assert.assertNotNull(right.getLocation(), "right was not placed");

    Assert.assertEquals(left.getLocation().first(), 0.0D);
    Assert.assertEquals(left.getSize().first(), 30.0D);

    Assert.assertEquals(right.getLocation().first(), 30.0D);
    Assert.assertEquals(right.getSize().first(), 40.0D);

    Assert.assertEquals(left.getLocation().second(), 0.0D);
    Assert.assertEquals(right.getLocation().second(), 0.0D);
  }

  public void testGapInsertedBetweenSerialChildren () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(7.0D).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(200, 100, Arrays.asList(left, right));

    Assert.assertEquals(right.getLocation().first(), 37.0D, "gap of 7 should be inserted between left (width 30) and right");
    Assert.assertEquals(layout.calculatePreferredWidth(), 77.0D, "preferred width should include the gap");
  }

  public void testParallelBoxStacksAtIdenticalPositions () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent top = new TestComponent("top", 10, 30, 100, 5, 20, 50);
    TestComponent bottom = new TestComponent("bottom", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.parallelBox(Alignment.FIRST).add(top).add(bottom));
    layout.setVerticalBox(layout.serialBox(0.0D).add(top).add(bottom));

    layout.doLayout(200, 200, Arrays.asList(top, bottom));

    Assert.assertEquals(top.getLocation().first(), 0.0D);
    Assert.assertEquals(bottom.getLocation().first(), 0.0D);
    Assert.assertEquals(top.getLocation().second(), 0.0D);
    Assert.assertEquals(bottom.getLocation().second(), 20.0D);
  }

  public void testRemoveAllClearsBothBoxes () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left));
    layout.setVerticalBox(layout.parallelBox().add(left));

    Assert.assertTrue(container.wasAdded(left));

    layout.removeAll();

    Assert.assertFalse(container.wasAdded(left));
  }

  public void testRemoveSingleComponent () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox().add(left).add(right));

    layout.remove(left);

    Assert.assertFalse(container.wasAdded(left));
    Assert.assertTrue(container.wasAdded(right));
  }

  @Test(expectedExceptions = LayoutException.class)
  public void testCalculateMinimumWidthThrowsWhenHorizontalBoxMissing () {

    ParaboxLayout layout = new ParaboxLayout(new StubContainer());

    layout.calculateMinimumWidth();
  }

  @Test(expectedExceptions = LayoutException.class)
  public void testCalculateMinimumHeightThrowsWhenVerticalBoxMissing () {

    ParaboxLayout layout = new ParaboxLayout(new StubContainer());

    layout.calculateMinimumHeight();
  }

  @Test(expectedExceptions = LayoutException.class)
  public void testDoLayoutThrowsWhenComponentNotInBothBoxes () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent stray = new TestComponent("stray", 10, 30, 100, 5, 20, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left));
    layout.setVerticalBox(layout.parallelBox().add(left));

    layout.doLayout(100, 100, Arrays.asList(left, stray));
  }

  public void testExplicitPerimeterIsHonoured () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(new Perimeter(2.0D, 3.0D, 4.0D, 5.0D), container);

    TestComponent only = new TestComponent("only", 10, 30, 100, 5, 20, 50);
    layout.setHorizontalBox(layout.serialBox(0.0D).add(only));
    layout.setVerticalBox(layout.parallelBox().add(only));

    Assert.assertEquals(layout.calculatePreferredWidth(), 30.0D + 3.0D + 5.0D);
    Assert.assertEquals(layout.calculatePreferredHeight(), 20.0D + 2.0D + 4.0D);

    layout.doLayout(100, 100, List.of(only));

    Assert.assertEquals(only.getLocation().first(), 3.0D);
    Assert.assertEquals(only.getLocation().second(), 2.0D);
  }

  public void testParallelBoxCenterAlignmentCentersWithinOverflow () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent only = new TestComponent("only", 10, 30, 100, 5, 20, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(only));
    layout.setVerticalBox(layout.parallelBox(Alignment.CENTER).add(only, Constraint.stretch()));

    layout.doLayout(200, 200, List.of(only));

    Assert.assertEquals(only.getSize().second(), 50.0D);
    Assert.assertEquals(only.getLocation().second(), (200.0D - 50.0D) / 2.0D);
  }

  public void testParallelBoxLastAlignmentPushesToTrailingEdge () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent only = new TestComponent("only", 10, 30, 100, 5, 20, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(only));
    layout.setVerticalBox(layout.parallelBox(Alignment.LAST).add(only, Constraint.stretch()));

    layout.doLayout(200, 200, List.of(only));

    Assert.assertEquals(only.getSize().second(), 50.0D);
    Assert.assertEquals(only.getLocation().second(), 200.0D - 50.0D);
  }

  public void testParallelBoxBaselineAlignsToIdealizedBaseline () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent tall = new TestComponent("tall", 10, 30, 100, 5, 20, 50).withBaselineAscent(40.0D);
    TestComponent shortComp = new TestComponent("short", 10, 30, 100, 5, 20, 50).withBaselineAscent(10.0D);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(tall).add(shortComp));
    layout.setVerticalBox(layout.parallelBox(Alignment.BASELINE).add(tall, Constraint.stretch()).add(shortComp, Constraint.stretch()));

    layout.doLayout(200, 200, Arrays.asList(tall, shortComp));

    Assert.assertEquals(tall.getSize().second(), 50.0D);
    Assert.assertEquals(shortComp.getSize().second(), 50.0D);
    Assert.assertEquals(shortComp.getLocation().second() - tall.getLocation().second(), 30.0D,
      "the element with the smaller baseline ascent is pushed down so its baseline aligns with the taller one");
  }

  public void testSerialBoxLastJustificationPlacesElementsBackwardFromTrailingEdge () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D, Justification.LAST).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(200, 100, Arrays.asList(left, right));

    Assert.assertEquals(left.getSize().first(), 30.0D);
    Assert.assertEquals(right.getSize().first(), 40.0D);
    Assert.assertEquals(left.getLocation().first(), 200.0D - 30.0D);
    Assert.assertEquals(right.getLocation().first(), 200.0D - 30.0D - 40.0D);
  }

  public void testSerialBoxCenterJustificationDistributesSlackEqually () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D, Justification.CENTER).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(200, 100, Arrays.asList(left, right));

    double slack = 200.0D - 30.0D - 40.0D;

    Assert.assertEquals(left.getLocation().first(), slack / 2.0D);
    Assert.assertEquals(right.getLocation().first(), (slack / 2.0D) + 30.0D);
  }

  public void testSerialBoxTrailingJustificationMirrorsLastBehaviorUnderDefaultOrientation () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D, Justification.TRAILING).add(left).add(right));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(200, 100, Arrays.asList(left, right));

    Assert.assertEquals(left.getLocation().first(), 200.0D - 30.0D);
    Assert.assertEquals(right.getLocation().first(), 200.0D - 30.0D - 40.0D);
  }

  public void testGrowConstraintAbsorbsSurplusUpToElementMaximum () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left).add(right, Constraint.expand()));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(150, 100, Arrays.asList(left, right));

    Assert.assertEquals(left.getSize().first(), 30.0D);
    Assert.assertEquals(right.getSize().first(), 100.0D, "right grows to its own maximum (100) and leaves the rest unused");
  }

  public void testShrinkConstraintReducesShrinkingMemberOnly () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left, Constraint.immutable()).add(right, Constraint.contract()));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(50, 100, Arrays.asList(left, right));

    Assert.assertEquals(left.getSize().first(), 30.0D, "non-shrinking element keeps preferred width");
    Assert.assertEquals(right.getSize().first(), 50.0D - 30.0D, "shrinking element absorbs the entire deficit");
  }

  public void testMutableConstraintSetGrowAndShrinkAreWired () {

    MutableConstraint constraint = Constraint.create().setGrow(0.7D).setShrink(0.3D);

    Assert.assertEquals(constraint.getGrow(), 0.7D);
    Assert.assertEquals(constraint.getShrink(), 0.3D);
  }

  public void testMutableConstraintMayGrowAndMayShrinkDefaultToHalf () {

    MutableConstraint constraint = Constraint.create().mayGrow().mayShrink();

    Assert.assertEquals(constraint.getGrow(), 0.5D);
    Assert.assertEquals(constraint.getShrink(), 0.5D);
  }

  public void testMutableConstraintParticipatesInLayoutLikeImmutableEquivalent () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    layout.setHorizontalBox(layout.serialBox(0.0D).add(left).add(right, Constraint.create().setGrow(0.5D)));
    layout.setVerticalBox(layout.parallelBox(Alignment.FIRST).add(left).add(right));

    layout.doLayout(150, 100, Arrays.asList(left, right));

    Assert.assertEquals(left.getSize().first(), 30.0D);
    Assert.assertEquals(right.getSize().first(), 100.0D, "MutableConstraint(grow=0.5) should let right grow to its maximum");
  }

  public void testNestedSerialBoxInsideParallelBoxLaysOutChildBoxComponents () {

    StubContainer container = new StubContainer();
    ParaboxLayout layout = new ParaboxLayout(container);

    TestComponent left = new TestComponent("left", 10, 30, 100, 5, 20, 50);
    TestComponent right = new TestComponent("right", 10, 40, 100, 5, 25, 50);

    SerialBox innerHorizontal = layout.serialBox(0.0D).add(left).add(right);
    ParallelBox outerHorizontal = layout.parallelBox(Alignment.FIRST).add(innerHorizontal);

    SerialBox innerVertical = layout.serialBox(0.0D).add(left).add(right);
    ParallelBox outerVertical = layout.parallelBox(Alignment.FIRST).add(innerVertical);

    layout.setHorizontalBox(outerHorizontal);
    layout.setVerticalBox(outerVertical);

    Assert.assertEquals(layout.calculatePreferredWidth(), 70.0D, "nested serial box reports sum of children");

    layout.doLayout(200, 200, Arrays.asList(left, right));

    Assert.assertEquals(left.getLocation().first(), 0.0D);
    Assert.assertEquals(right.getLocation().first(), 30.0D);
  }

  static final class TestComponent {

    private final String label;
    private final double minWidth;
    private final double prefWidth;
    private final double maxWidth;
    private final double minHeight;
    private final double prefHeight;
    private final double maxHeight;
    private Pair location;
    private Pair size;
    private double baselineAscent;

    TestComponent (String label, double minWidth, double prefWidth, double maxWidth, double minHeight, double prefHeight, double maxHeight) {

      this.label = label;
      this.minWidth = minWidth;
      this.prefWidth = prefWidth;
      this.maxWidth = maxWidth;
      this.minHeight = minHeight;
      this.prefHeight = prefHeight;
      this.maxHeight = maxHeight;
    }

    TestComponent withBaselineAscent (double baselineAscent) {

      this.baselineAscent = baselineAscent;

      return this;
    }

    double getBaselineAscent () {

      return baselineAscent;
    }

    double width (Bias bias, TapeMeasure tapeMeasure) {

      if (bias == Bias.HORIZONTAL) {
        return switch (tapeMeasure) {
          case MINIMUM -> minWidth;
          case PREFERRED -> prefWidth;
          case MAXIMUM -> maxWidth;
        };
      } else {
        return switch (tapeMeasure) {
          case MINIMUM -> minHeight;
          case PREFERRED -> prefHeight;
          case MAXIMUM -> maxHeight;
        };
      }
    }

    void place (Pair location, Pair size) {

      this.location = location;
      this.size = size;
    }

    Pair getLocation () {

      return location;
    }

    Pair getSize () {

      return size;
    }

    @Override
    public String toString () {

      return label;
    }
  }

  static final class StubPlatform implements ParaboxPlatform {

    @Override
    public double getRelatedGap () {

      return 5.0D;
    }

    @Override
    public double getUnrelatedGap () {

      return 10.0D;
    }

    @Override
    public Perimeter getFramePerimeter () {

      return new Perimeter(0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public Orientation getOrientation () {

      return Orientation.getDefaultOrientation();
    }
  }

  static final class StubContainer implements ParaboxContainer<TestComponent> {

    private final ParaboxPlatform platform = new StubPlatform();
    private final Map<TestComponent, Boolean> added = new HashMap<>();

    @Override
    public ParaboxPlatform getPlatform () {

      return platform;
    }

    @Override
    public ParaboxElement<TestComponent> constructElement (TestComponent component, Constraint constraint) {

      return new ComponentParaboxElement<TestComponent>(component, constraint) {

        @Override
        public double getComponentMinimumMeasurement (Bias bias) {

          return component.width(bias, TapeMeasure.MINIMUM);
        }

        @Override
        public double getComponentPreferredMeasurement (Bias bias) {

          return component.width(bias, TapeMeasure.PREFERRED);
        }

        @Override
        public double getComponentMaximumMeasurement (Bias bias) {

          return component.width(bias, TapeMeasure.MAXIMUM);
        }

        @Override
        public double getBaseline (Bias bias, double measurement) {

          return component.getBaselineAscent();
        }

        @Override
        public void applyLayout (Pair location, Pair size) {

          component.place(location, size);
        }
      };
    }

    @Override
    public void nativelyAddComponent (TestComponent component) {

      added.put(component, true);
    }

    @Override
    public void nativelyRemoveComponent (TestComponent component) {

      added.remove(component);
    }

    boolean wasAdded (TestComponent component) {

      return added.getOrDefault(component, false);
    }
  }
}
