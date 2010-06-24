package org.smallmind.scribe.trial;

import org.smallmind.scribe.pen.ClassNameTemplate;
import org.smallmind.scribe.pen.ConsoleAppender;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.Template;
import org.smallmind.scribe.pen.XMLFormatter;
import org.smallmind.scribe.pen.aop.AutoLog;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LoggerTest {

   private Logger logger;

   @BeforeClass
   public void setup ()
      throws LoggerException {

      Template testTemplate;

      testTemplate = new ClassNameTemplate("org.smallmind.scribe.trial.LoggerTest");
      testTemplate.setAutoFillLogicalContext(true);
      testTemplate.addAppender(new ConsoleAppender(new XMLFormatter()));

      LoggerManager.addTemplate(testTemplate);
      logger = LoggerManager.getLogger(LoggerTest.class);
   }

   @AutoLog
   @Test
   public void testLogger () {

      logger.info("This is a test method");
   }
}
