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
package org.smallmind.web.json.scaffold.util;

import java.util.LinkedList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link Page} construction from arrays and lists, the empty-page factory, element mutation,
 * Jackson re-binding via {@code jsonConvert}, and iteration.
 */
@Test(groups = "unit")
public class PageTest {

  public void testArrayConstructorDerivesResultSize () {

    Page<Integer> page = new Page<>(new Integer[] {1, 2, 3}, 10L, 25, 100L);

    Assert.assertEquals(page.getResultSize(), 3);
    Assert.assertEquals(page.getFirstResult(), 10L);
    Assert.assertEquals(page.getMaxResults(), 25);
    Assert.assertEquals(page.getTotalResults(), 100L);
  }

  public void testListConstructorDerivesResultSize () {

    List<String> values = new LinkedList<>(List.of("a", "b"));
    Page<String> page = new Page<>(values, 0L, 20, 2L);

    Assert.assertEquals(page.getResultSize(), 2);
  }

  public void testEmptyFactory () {

    Page<String> page = Page.empty(String.class);

    Assert.assertEquals(page.getResultSize(), 0);
    Assert.assertEquals(page.getValues().length, 0);
    Assert.assertEquals(page.getValues().getClass().getComponentType(), String.class);
  }

  public void testMutateTransformsElementsPreservingMetadata ()
    throws Exception {

    Page<Integer> page = new Page<>(new Integer[] {1, 2, 3}, 5L, 10, 42L);
    Page<String> mutated = page.mutate(String.class, value -> "n" + value);

    Assert.assertEquals(mutated.getValues(), new String[] {"n1", "n2", "n3"});
    Assert.assertEquals(mutated.getFirstResult(), 5L);
    Assert.assertEquals(mutated.getTotalResults(), 42L);
  }

  public void testListConstructedPageHasTypedBackingArray () {

    Widget first = new Widget();
    first.setLabel("gear");
    Widget second = new Widget();
    second.setLabel("sprocket");

    Page<Widget> page = new Page<>(List.of(first, second), 0L, 10, 2L);

    // The list constructor now derives the element type, so typed access succeeds without a prior
    // jsonConvert (a generic getValues() no longer trips a synthetic checkcast against Object[]).
    Assert.assertEquals(page.getValues().getClass().getComponentType(), Widget.class);
    Assert.assertEquals(page.getValues()[0].getLabel(), "gear");
    Assert.assertEquals(page.getValues()[1].getLabel(), "sprocket");
  }

  public void testHeterogeneousListFallsBackToObjectArray () {

    // Mixed element types cannot share a narrower component type, so the backing array stays Object[].
    List<Object> mixed = new LinkedList<>();
    mixed.add("text");
    mixed.add(42);

    Page rawPage = new Page<>(mixed, 0L, 10, 2L);

    Assert.assertEquals(rawPage.getValues().getClass().getComponentType(), Object.class);
  }

  public void testJsonConvertRebindsComponentType () {

    List<Object> source = new LinkedList<>();
    source.add(java.util.Map.of("label", "gear"));

    Page rawPage = new Page<>(source, 0L, 10, 1L);

    rawPage.jsonConvert(Widget.class);

    Assert.assertEquals(rawPage.getValues().getClass().getComponentType(), Widget.class);
    Assert.assertEquals(((Widget)rawPage.getValues()[0]).getLabel(), "gear");
  }

  public void testIteration () {

    Page<Integer> page = new Page<>(new Integer[] {7, 8}, 0L, 10, 2L);
    int sum = 0;

    for (Integer value : page) {
      sum += value;
    }

    Assert.assertEquals(sum, 15);
  }

  public static class Widget {

    private String label;

    public String getLabel () {

      return label;
    }

    public void setLabel (String label) {

      this.label = label;
    }
  }
}
