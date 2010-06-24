package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.slf4j.ScribeLoggerFactory;

public class StaticLoggerBinder implements LoggerFactoryBinder {

   public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

   private final ILoggerFactory loggerFactory;

   static {

      LoggerManager.addLoggingPackagePrefix("org.slf4j.");
   }

   public StaticLoggerBinder () {

      loggerFactory = new ScribeLoggerFactory();
   }

   public ILoggerFactory getLoggerFactory () {

      return loggerFactory;
   }

   public String getLoggerFactoryClassStr () {

      return ScribeLoggerFactory.class.getName();
   }
}