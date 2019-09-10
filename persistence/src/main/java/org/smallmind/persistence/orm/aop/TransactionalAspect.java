/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import org.aspectj.lang.annotation.DeclarePrecedence;

@Aspect
@DeclarePrecedence("org.smallmind.nutsnbolts.inject.LazyFieldAspect, org.smallmind.persistence.orm.aop.TransactionalAspect")
public class TransactionalAspect {

  @Before(value = "@within(transactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(Transactional)", argNames = "transactional")
  public void beforeTransactionalClass (Transactional transactional) {

    TransactionalState.startBoundary(transactional);
  }

  @Before(value = "(execution(@Transactional * * (..)) || initialization(@Transactional new(..))) && @annotation(transactional)", argNames = "transactional")
  public void beforeTransactionalMethod (Transactional transactional) {

    TransactionalState.startBoundary(transactional);
  }

  @AfterReturning(pointcut = "@within(transactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(Transactional)", argNames = "transactional")
  public void afterReturnFromTransactionalClass (Transactional transactional) {

    TransactionalState.commitBoundary();
  }

  @AfterReturning(pointcut = "(execution(@Transactional * * (..)) || initialization(@Transactional new(..))) && @annotation(transactional)", argNames = "transactional")
  public void afterReturnFromTransactionalMethod (Transactional transactional) {

    TransactionalState.commitBoundary();
  }

  @AfterThrowing(pointcut = "@within(transactional) && (execution(* * (..)) || initialization(new(..)))  && !@annotation(Transactional)", throwing = "throwable", argNames = "transactional, throwable")
  public void afterThrowFromTransactionalClass (Transactional transactional, Throwable throwable) {

    if (transactional.rollbackOnException()) {
      TransactionalState.rollbackBoundary(throwable);
    } else {
      TransactionalState.commitBoundary(throwable);
    }
  }

  @AfterThrowing(pointcut = "(execution(@Transactional * * (..)) || initialization(@Transactional new(..))) && @annotation(transactional)", throwing = "throwable", argNames = "transactional, throwable")
  public void afterThrowFromTransactionalMethod (Transactional transactional, Throwable throwable) {

    if (transactional.rollbackOnException()) {
      TransactionalState.rollbackBoundary(throwable);
    } else {
      TransactionalState.commitBoundary(throwable);
    }
  }
}
