package org.smallmind.scribe.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.smallmind.scribe.pen.LoggerManager;

public class ScribeLoggerFactory implements ILoggerFactory {

   public Logger getLogger (String name) {

      return new ScribeLoggerAdapter(LoggerManager.getLogger(name));
   }
}
