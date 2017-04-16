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
package org.smallmind.scribe.pen.aop;

import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.nutsnbolts.context.Context;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

@Aspect
public class LogContextAspect {

  private static final Class[] PARSE_SIGNATURE = new Class[] {String.class};

  @AfterReturning(value = "execution(@LogContext * * (..)) && @annotation(logContext)", argNames = "staticPart, logContext")
  public void afterReturnFromLogContextMethod (JoinPoint.StaticPart staticPart, LogContext logContext) {

    Logger backingLogger;
    Discriminator discriminator = null;

    backingLogger = LoggerManager.getLogger(logContext.name().equals("") ? staticPart.getSourceLocation().getWithinType().getCanonicalName() : logContext.name());

    if (!logContext.discriminator().ofClass().equals(Unused.class)) {

      Method parseMethod;

      try {
        parseMethod = logContext.discriminator().ofClass().getMethod("parse", PARSE_SIGNATURE);
        discriminator = (Discriminator)parseMethod.invoke(null, logContext.discriminator().value());
      }
      catch (Exception exception) {
        throw new AutoLogRuntimeException(exception);
      }
    }

    for (Class<? extends Context> contextClass : logContext.context()) {

      Context context;

      if ((context = ContextFactory.getContext(contextClass)) != null) {

        backingLogger.log(discriminator, logContext.off() ? Level.OFF : (!logContext.level().equals(Level.OFF)) ? logContext.level() : backingLogger.getLevel(), context);
      }
    }
  }
}
