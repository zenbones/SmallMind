/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.sleuth.runner;

import java.util.Arrays;

public class SleuthRunner {

  public static void execute (int maxThreads, String[] groups, Class<?>... classes)
    throws InterruptedException {

    execute(maxThreads, groups, Arrays.asList(classes));
  }

  public static void execute (int maxThreads, String[] groups, Iterable<Class<?>> classIterable)
    throws InterruptedException {

    if (classIterable != null) {

      AnnotationProcessor annotationProcessor = new AnnotationProcessor(new NativeAnnotationTranslator(), new TestNGAnnotationTranslator());
      TestThreadPool threadPool = new TestThreadPool((maxThreads <= 0) ? Integer.MAX_VALUE : maxThreads);
      DependencyAnalysis<Suite, Class<?>> suiteAnalysis = new DependencyAnalysis<>(Suite.class);
      DependencyQueue<Suite, Class<?>> suiteDependencyQueue;
      Dependency<Suite, Class<?>> suiteDependency;

      for (Class<?> clazz : classIterable) {

        AnnotationDictionary annotationDictionary;

        if ((annotationDictionary = annotationProcessor.process(clazz)) != null) {
          if (annotationDictionary.getSuite().enabled() && ((groups == null) || inGroups(annotationDictionary.getSuite().group(), groups))) {
            suiteAnalysis.add(new Dependency<>(clazz.getName(), annotationDictionary.getSuite(), clazz, annotationDictionary.getSuite().priority(), annotationDictionary.getSuite().dependsOn()));
          }
        }
      }

      suiteDependencyQueue = suiteAnalysis.calculate();
      while ((suiteDependency = suiteDependencyQueue.poll()) != null) {
        try {
          threadPool.execute(TestTier.SUITE, new SuiteRunner(suiteDependency, suiteDependencyQueue, annotationProcessor, threadPool));
        } catch (Exception exception) {
//TODO: Test Failure
        }
      }

      threadPool.await(suiteDependencyQueue.getSize());
    }
  }

  private static boolean inGroups (String name, String[] names) {

    if ((name != null) && (names != null)) {
      for (String possibility : names) {
        if (name.equals(possibility)) {

          return true;
        }
      }
    }

    return false;
  }
}
