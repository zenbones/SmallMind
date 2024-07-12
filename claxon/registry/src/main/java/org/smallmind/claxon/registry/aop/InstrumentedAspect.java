/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.claxon.registry.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.nutsnbolts.reflection.aop.AOPUtility;
import org.smallmind.nutsnbolts.util.WithResultExecutable;

@Aspect
public class InstrumentedAspect {

  @Around(value = "(execution(@Instrumented * * (..)) || initialization(@Instrumented new(..))) && @annotation(instrumented)", argNames = "thisJoinPoint, instrumented")
  public Object aroundInstrumentedMethod (ProceedingJoinPoint thisJoinPoint, Instrumented instrumented)
    throws Throwable {

    if (!instrumented.active()) {

      return thisJoinPoint.proceed();
    } else {

      MeterBuilder<?> builder;
      Tag[] tags = new Tag[instrumented.constants().length + instrumented.parameters().length];
      Class<?> caller = Instrumented.class.equals(instrumented.caller()) ? thisJoinPoint.getStaticPart().getSourceLocation().getWithinType() : instrumented.caller();
      int index = 0;

      for (ConstantTag constantTag : instrumented.constants()) {
        tags[index++] = new Tag(constantTag.key(), constantTag.constant());
      }
      for (ParameterTag parameterTag : instrumented.parameters()) {
        tags[index++] = new Tag(parameterTag.key(), AOPUtility.getParameterValue(thisJoinPoint, parameterTag.parameter(), false).toString());
      }

      builder = new InstrumentedLazyBuilder(instrumented.parser(), instrumented.json());

      return Instrument.with(caller, builder, tags)
        .as(instrumented.timeUnit())
        .on((WithResultExecutable<Object>)thisJoinPoint::proceed);
    }
  }
}