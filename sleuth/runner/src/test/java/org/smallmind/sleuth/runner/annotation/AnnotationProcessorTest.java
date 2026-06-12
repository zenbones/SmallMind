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
package org.smallmind.sleuth.runner.annotation;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AnnotationProcessorTest {

  private static AnnotationDictionary implementedDictionary () {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.setSuite(new SuiteLiteral());

    return dictionary;
  }

  public void testFirstImplementedTranslatorWinsAndShortCircuits () {

    CountingTranslator winner = new CountingTranslator(implementedDictionary());
    CountingTranslator neverConsulted = new CountingTranslator(implementedDictionary());
    AnnotationProcessor processor = new AnnotationProcessor(winner, neverConsulted);

    AnnotationDictionary resolved = processor.process(Subject.class);

    Assert.assertSame(resolved, winner.getDictionary());
    Assert.assertEquals(winner.getCallCount(), 1);
    Assert.assertEquals(neverConsulted.getCallCount(), 0, "Later translators must not be consulted once one is implemented");
  }

  public void testUnimplementedTranslatorIsSkipped () {

    CountingTranslator empty = new CountingTranslator(new AnnotationDictionary());
    CountingTranslator implemented = new CountingTranslator(implementedDictionary());
    AnnotationProcessor processor = new AnnotationProcessor(empty, implemented);

    AnnotationDictionary resolved = processor.process(Subject.class);

    Assert.assertSame(resolved, implemented.getDictionary());
    Assert.assertEquals(empty.getCallCount(), 1);
    Assert.assertEquals(implemented.getCallCount(), 1);
  }

  public void testResultIsCachedPerClass () {

    CountingTranslator implemented = new CountingTranslator(implementedDictionary());
    AnnotationProcessor processor = new AnnotationProcessor(implemented);

    AnnotationDictionary first = processor.process(Subject.class);
    AnnotationDictionary second = processor.process(Subject.class);

    Assert.assertSame(second, first);
    Assert.assertEquals(implemented.getCallCount(), 1, "A cached class must not be re-scanned");
  }

  public void testReturnsNullWhenNoTranslatorIsImplemented () {

    AnnotationProcessor processor = new AnnotationProcessor(new CountingTranslator(new AnnotationDictionary()), new CountingTranslator(new AnnotationDictionary()));

    Assert.assertNull(processor.process(Subject.class));
  }

  // A translator that hands back a fixed dictionary and counts how many times it was asked, so the
  // processor's short-circuit and caching behaviour can be observed.
  private static class CountingTranslator implements AnnotationTranslator {

    private final AnnotationDictionary dictionary;
    private int callCount;

    private CountingTranslator (AnnotationDictionary dictionary) {

      this.dictionary = dictionary;
    }

    private AnnotationDictionary getDictionary () {

      return dictionary;
    }

    private int getCallCount () {

      return callCount;
    }

    @Override
    public AnnotationDictionary process (Class<?> clazz) {

      callCount++;

      return dictionary;
    }
  }

  private static class Subject {

  }
}
