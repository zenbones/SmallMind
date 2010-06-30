package org.smallmind.persistence.orm.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class NonTransactionalAspect {

   @Before (value = "@within(nonTransactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(NonTransactional)", argNames = "nonTransactional")
   public void beforeNonTransactionalClass (NonTransactional nonTransactional) {

      NonTransactionalState.startBoundary(nonTransactional);
   }

   @Before (value = "(execution(@NonTransactional * * (..)) || initialization(@NonTransactional new(..))) && @annotation(nonTransactional)", argNames = "nonTransactional")
   public void beforeNonTransactionalMethod (NonTransactional nonTransactional) {

      NonTransactionalState.startBoundary(nonTransactional);
   }

   @AfterReturning (value = "@within(NonTransactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(NonTransactional)")
   public void afterReturnFromNonTransactionalClass () {

      NonTransactionalState.endBoundary(null);
   }

   @AfterReturning (value = "(execution(@NonTransactional * * (..)) || initialization(@NonTransactional new(..))) && @annotation(NonTransactional)")
   public void afterReturnFromNonTransactionalMethod () {

      NonTransactionalState.endBoundary(null);
   }

   @AfterThrowing (value = "@within(NonTransactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(NonTransactional)", throwing = "throwable")
   public void afterThrowFromNonTransactionalClass (Throwable throwable) {

      NonTransactionalState.endBoundary(throwable);
   }

   @AfterThrowing (value = "(execution(@NonTransactional * * (..)) || initialization(@NonTransactional new(..))) && @annotation(NonTransactional)", throwing = "throwable")
   public void afterThrowFromNonTransactionalMethod (Throwable throwable) {

      NonTransactionalState.endBoundary(throwable);
   }
}