package org.smallmind.scribe.pen;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class RegExpTemplate extends Template {

   private AtomicReference<Pattern> loggerPatternRef = new AtomicReference<Pattern>();

   public RegExpTemplate () {

      super();
   }

   public RegExpTemplate (String expression) {

      super();

      loggerPatternRef.set(Pattern.compile(expression));
   }

   public RegExpTemplate (Level level, boolean autoFillLogicalContext, String expression)
      throws LoggerException {

      super(level, autoFillLogicalContext);

      loggerPatternRef.set(Pattern.compile(expression));
   }

   public RegExpTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLogicalContext, String expression)
      throws LoggerException {

      super(filters, appenders, enhancers, level, autoFillLogicalContext);

      loggerPatternRef.set(Pattern.compile(expression));
   }

   public void setExpression (String expression) {

      if (!loggerPatternRef.compareAndSet(null, Pattern.compile(expression))) {
         throw new LoggerRuntimeException("RegExpTemplate has been previously initialized with a pattern");
      }
   }

   public int matchLogger (String loggerName) {

      if (loggerPatternRef.get() == null) {
         throw new LoggerRuntimeException("RegExpTemplate was never initialized with a pattern");
      }

      return loggerPatternRef.get().matcher(loggerName).matches() ? Integer.MAX_VALUE : NO_MATCH;
   }
}