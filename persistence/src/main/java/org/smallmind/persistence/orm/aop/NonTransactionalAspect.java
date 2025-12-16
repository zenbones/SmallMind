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
package org.smallmind.persistence.orm.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Aspect that establishes and tears down non-transactional boundaries around annotated types and methods.
 */
@Aspect
public class NonTransactionalAspect {

  /**
   * Starts a non-transactional boundary for any execution within a @NonTransactional class (unless overridden on the method).
   */
  @Before(value = "@within(nonTransactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(NonTransactional)", argNames = "nonTransactional")
  public void beforeNonTransactionalClass (NonTransactional nonTransactional) {

    NonTransactionalState.startBoundary(nonTransactional);
  }

  /**
   * Starts a non-transactional boundary for an explicitly annotated constructor or method.
   */
  @Before(value = "(execution(@NonTransactional * * (..)) || initialization(@NonTransactional new(..))) && @annotation(nonTransactional)", argNames = "nonTransactional")
  public void beforeNonTransactionalMethod (NonTransactional nonTransactional) {

    NonTransactionalState.startBoundary(nonTransactional);
  }

  /**
   * Ends the boundary after successful execution within a @NonTransactional class.
   */
  @AfterReturning(value = "@within(NonTransactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(NonTransactional)")
  public void afterReturnFromNonTransactionalClass () {

    NonTransactionalState.endBoundary(null);
  }

  /**
   * Ends the boundary after successful execution of an explicitly annotated member.
   */
  @AfterReturning(value = "(execution(@NonTransactional * * (..)) || initialization(@NonTransactional new(..))) && @annotation(NonTransactional)")
  public void afterReturnFromNonTransactionalMethod () {

    NonTransactionalState.endBoundary(null);
  }

  /**
   * Ends the boundary when an exception escapes from a @NonTransactional class.
   */
  @AfterThrowing(value = "@within(NonTransactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(NonTransactional)", throwing = "throwable")
  public void afterThrowFromNonTransactionalClass (Throwable throwable) {

    NonTransactionalState.endBoundary(throwable);
  }

  /**
   * Ends the boundary when an exception escapes from an explicitly annotated member.
   */
  @AfterThrowing(value = "(execution(@NonTransactional * * (..)) || initialization(@NonTransactional new(..))) && @annotation(NonTransactional)", throwing = "throwable")
  public void afterThrowFromNonTransactionalMethod (Throwable throwable) {

    NonTransactionalState.endBoundary(throwable);
  }
}
