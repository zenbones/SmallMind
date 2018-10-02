/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestNGAnnotationTranslator implements AnnotationTranslator {

  @Override
  public AnnotationDictionary process (Class<?> clazz) {

    AnnotationDictionary annotationDictionary = new AnnotationDictionary();

    Test test;

    if ((test = clazz.getAnnotation(Test.class)) != null) {
      annotationDictionary.setSuite(new SuiteLiteral((test.groups().length == 0) ? null : test.groups(), test.priority(), new String[0], test.dependsOnMethods(), test.enabled()));
    }
    for (Method method : clazz.getMethods()) {
      for (Annotation annotation : method.getAnnotations()) {
        if (annotation instanceof BeforeClass) {
          annotationDictionary.addBeforeSuiteMethod(method, new BeforeSuiteLiteral());
        }
        if (annotation instanceof AfterClass) {
          annotationDictionary.addAfterSuiteMethod(method, new AfterSuiteLiteral());
        }
        if (annotation instanceof BeforeMethod) {
          annotationDictionary.addBeforeTestMethod(method, new BeforeTestLiteral());
        }
        if (annotation instanceof AfterMethod) {
          annotationDictionary.addAfterTestMethod(method, new AfterTestLiteral());
        }
        if (annotation instanceof Test) {
          annotationDictionary.addTestMethod(method, new TestLiteral(((Test)annotation).priority(), new String[0], ((Test)annotation).dependsOnMethods(), ((Test)annotation).enabled()));
        }
      }
    }

    return annotationDictionary;
  }
}
