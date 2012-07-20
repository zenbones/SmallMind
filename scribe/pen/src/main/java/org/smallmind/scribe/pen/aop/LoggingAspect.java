/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.probe.Probe;
import org.smallmind.scribe.pen.probe.ProbeFactory;

@Aspect
public class LoggingAspect {

  private static final Class[] PARSE_SIGNATURE = new Class[] {String.class};

  private void startProbe (AutoLog autoLog, Class toBeLoggedClass) {

    Logger backingLogger;
    Probe autoProbe;
    Discriminator discriminator = null;

    if (!autoLog.discriminator().ofClass().equals(Unused.class)) {

      Method parseMethod;

      try {
        parseMethod = autoLog.discriminator().ofClass().getMethod("parse", PARSE_SIGNATURE);
        discriminator = (Discriminator)parseMethod.invoke(null, autoLog.discriminator().value());
      }
      catch (Exception exception) {
        throw new AutoLogRuntimeException(exception);
      }
    }

    backingLogger = LoggerManager.getLogger(autoLog.name().equals("") ? toBeLoggedClass.getCanonicalName() : autoLog.name());
    autoProbe = ProbeFactory.createProbe(backingLogger, discriminator, autoLog.off() ? Level.OFF : (!autoLog.level().equals(Level.OFF)) ? autoLog.level() : backingLogger.getLevel(), (autoLog.title().length() == 0) ? null : autoLog.title());
    autoProbe.start();
  }

  private void stopProbe () {

    Probe probe;

    probe = ProbeFactory.getProbe();
    if (probe == null) {
      throw new AutoLogRuntimeException("The current thread has no stored Probe");
    }

    if (!probe.isAborted()) {
      probe.stop();
    }
  }

  private void abortProbe (Throwable throwable) {

    Probe probe;

    probe = ProbeFactory.getProbe();
    if (probe == null) {
      throw new AutoLogRuntimeException("The current thread has no stored Probe");
    }

    if (throwable.equals(ProbeFactory.getThrowable())) {
      probe.abort();
    }
    else {
      ProbeFactory.setThrowable(throwable);
      probe.abort(throwable);
    }
  }

  @Before(value = "@within(autoLog) && (execution(* * (..)) || initialization(new(..)) || staticinitialization(*)) && !@annotation(AutoLog)", argNames = "staticPart, autoLog")
  public void beforeClassToBeLogged (JoinPoint.StaticPart staticPart, AutoLog autoLog) {

    startProbe(autoLog, staticPart.getSourceLocation().getWithinType());
  }

  @Before(value = "(execution(@AutoLog * * (..)) || initialization(@AutoLog new(..))) && @annotation(autoLog)", argNames = "staticPart, autoLog")
  public void beforeMethodToBeLogged (JoinPoint.StaticPart staticPart, AutoLog autoLog) {

    startProbe(autoLog, staticPart.getSourceLocation().getWithinType());
  }

  @AfterReturning("@within(AutoLog) && (execution(* * (..)) || initialization(new(..)) || staticinitialization(*)) && !@annotation(AutoLog)")
  public void afterReturnFromClassToBeLogged () {

    stopProbe();
  }

  @AfterReturning("(execution(@AutoLog * * (..)) || initialization(@AutoLog new(..))) && @annotation(AutoLog)")
  public void afterReturnFromMethodToBeLogged () {

    stopProbe();
  }

  @AfterThrowing(pointcut = "@within(AutoLog) && (execution(* * (..)) || initialization(new(..)) || staticinitialization(*))  && !@annotation(AutoLog)", throwing = "throwable", argNames = "throwable")
  public void afterThrowFromClassToBeLogged (Throwable throwable) {

    abortProbe(throwable);
  }

  @AfterThrowing(pointcut = "(execution(@AutoLog * * (..)) || initialization(@AutoLog new(..))) && @annotation(AutoLog)", throwing = "throwable", argNames = "throwable")
  public void afterThrowFromMethodToBeLogged (Throwable throwable) {

    abortProbe(throwable);
  }
}