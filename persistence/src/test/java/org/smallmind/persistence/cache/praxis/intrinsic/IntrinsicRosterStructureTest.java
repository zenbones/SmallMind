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
package org.smallmind.persistence.cache.praxis.intrinsic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies that {@link IntrinsicRosterStructure} propagates head, tail, and size changes between a parent
 * {@link IntrinsicRoster} and its sublist views in both directions, as the parent and the sublist share the
 * same backing node chain, lock, and (chained) structure.
 */
@Test(groups = "unit")
public class IntrinsicRosterStructureTest {

  private static IntrinsicRoster<String> roster (String... elements) {

    return new IntrinsicRoster<>(Arrays.asList(elements));
  }

  private static List<String> contentsOf (IntrinsicRoster<String> roster) {

    List<String> contents = new ArrayList<>();

    for (int index = 0; index < roster.size(); index++) {
      contents.add(roster.get(index));
    }

    return contents;
  }

  private static List<String> contentsOf (List<String> view) {

    List<String> contents = new ArrayList<>();

    for (int index = 0; index < view.size(); index++) {
      contents.add(view.get(index));
    }

    return contents;
  }

  public void testSubListViewReportsScopedSizeAndElements () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    Assert.assertEquals(view.size(), 2);
    Assert.assertEquals(contentsOf(view), Arrays.asList("b", "c"));
  }

  public void testAddIntoSubListGrowsParentAndShowsThroughInOrder () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    // Append within the view: the new node lands between the view's tail (c) and the parent's d.
    view.add("x");

    Assert.assertEquals(view.size(), 3);
    Assert.assertEquals(contentsOf(view), Arrays.asList("b", "c", "x"));

    Assert.assertEquals(roster.size(), 5);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "b", "c", "x", "d"));
  }

  public void testInsertAtFrontOfSubListMovesViewHeadAndKeepsParentOrder () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    // Insert at index 0 of the view: the new node becomes the view's head, before b but after a in the parent.
    view.add(0, "y");

    Assert.assertEquals(view.size(), 3);
    Assert.assertEquals(contentsOf(view), Arrays.asList("y", "b", "c"));

    Assert.assertEquals(roster.size(), 5);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "y", "b", "c", "d"));
  }

  public void testRemoveFromSubListShrinksParentAndShowsThrough () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    Assert.assertTrue(view.remove("b"));

    Assert.assertEquals(view.size(), 1);
    Assert.assertEquals(contentsOf(view), Arrays.asList("c"));

    Assert.assertEquals(roster.size(), 3);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "c", "d"));
  }

  public void testRemoveSubListTailReassignsBoundaryAndParentReflectsIt () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    Assert.assertTrue(view.remove("c"));

    Assert.assertEquals(view.size(), 1);
    Assert.assertEquals(contentsOf(view), Arrays.asList("b"));

    Assert.assertEquals(roster.size(), 3);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "b", "d"));
  }

  public void testClearOnSubListRemovesItsSpanFromTheParent () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    view.clear();

    Assert.assertTrue(view.isEmpty());
    Assert.assertEquals(view.size(), 0);

    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "d"));
  }

  public void testParentMutationIsVisibleThroughSubListView () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d");
    List<String> view = roster.subList(1, 3);

    // Replacing a node's value in the parent is seen through the shared nodes of the view.
    roster.set(2, "C");

    Assert.assertEquals(contentsOf(view), Arrays.asList("b", "C"));
  }

  public void testAddIntoEmptySubListReconstitutesAndCascadesToParent () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    // An empty (fromIndex == toIndex) view sits between b and c; adding triggers ouroboros on the
    // empty view, which cascades through reconstitute into the parent.
    List<String> view = roster.subList(1, 1);

    Assert.assertTrue(view.isEmpty());

    view.add("x");

    Assert.assertEquals(view.size(), 1);
    Assert.assertEquals(contentsOf(view), Arrays.asList("x"));

    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "b", "x", "c"));
  }

  public void testAddFirstIntoEmptySubListReconstitutesAndCascadesToParent () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    IntrinsicRoster<String> view = (IntrinsicRoster<String>)roster.subList(1, 1);

    view.addFirst("x");

    Assert.assertEquals(view.size(), 1);
    Assert.assertEquals(contentsOf(view), Arrays.asList("x"));

    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "b", "x", "c"));
  }

  public void testInsertAtSubListHeadCascadesSetHeadToParent () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    // The view's head is also the parent's head; inserting before it moves both heads.
    List<String> view = roster.subList(0, 2);

    view.add(0, "z");

    Assert.assertEquals(contentsOf(view), Arrays.asList("z", "a", "b"));

    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("z", "a", "b", "c"));
    Assert.assertEquals(roster.getFirst(), "z");
  }

  public void testAppendAtSubListTailCascadesSetTailToParent () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    // The view's tail is also the parent's tail; appending past it moves both tails.
    List<String> view = roster.subList(1, 3);

    view.add("z");

    Assert.assertEquals(contentsOf(view), Arrays.asList("b", "c", "z"));

    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(contentsOf(roster), Arrays.asList("a", "b", "c", "z"));
    Assert.assertEquals(roster.getLast(), "z");
  }
}
