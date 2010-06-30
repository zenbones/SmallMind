package org.smallmind.persistence.orm.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class TransactionalAspect {

   @Before (value = "@within(transactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(Transactional)", argNames = "transactional")
   public void beforeTransactionalClass (Transactional transactional) {

      TransactionalState.startBoundary(transactional);
   }

   @Before (value = "(execution(@Transactional * * (..)) || initialization(@Transactional new(..))) && @annotation(transactional)", argNames = "transactional")
   public void beforeTransactionalMethod (Transactional transactional) {

      TransactionalState.startBoundary(transactional);
   }

   @AfterReturning (pointcut = "@within(transactional) && (execution(* * (..)) || initialization(new(..))) && !@annotation(Transactional)", argNames = "transactional")
   public void afterReturnFromTransactionalClass (Transactional transactional) {

      TransactionalState.commitBoundary();
   }

   @AfterReturning (pointcut = "(execution(@Transactional * * (..)) || initialization(@Transactional new(..))) && @annotation(transactional)", argNames = "transactional")
   public void afterReturnFromTransactionalMethod (Transactional transactional) {

      TransactionalState.commitBoundary();
   }

   @AfterThrowing (pointcut = "@within(transactional) && (execution(* * (..)) || initialization(new(..)))  && !@annotation(Transactional)", throwing = "throwable", argNames = "transactional, throwable")
   public void afterThrowFromTransactionalClass (Transactional transactional, Throwable throwable) {

      if (transactional.rollbackOnException()) {
         TransactionalState.rollbackBoundary(throwable);
      }
      else {
         TransactionalState.commitBoundary(throwable);
      }
   }

   @AfterThrowing (pointcut = "(execution(@Transactional * * (..)) || initialization(@Transactional new(..))) && @annotation(transactional)", throwing = "throwable", argNames = "transactional, throwable")
   public void afterThrowFromTransactionalMethod (Transactional transactional, Throwable throwable) {

      if (transactional.rollbackOnException()) {
         TransactionalState.rollbackBoundary(throwable);
      }
      else {
         TransactionalState.commitBoundary(throwable);
      }
   }
}
