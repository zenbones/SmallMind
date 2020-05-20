package org.smallmind.claxon.collector.indigenous;

import java.util.function.Consumer;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

public class ScribeLoggerConsumer implements Consumer<String> {

  private final Logger logger;
  private final Level level;

  public ScribeLoggerConsumer (Level level) {

    this(null, level);
  }

  public ScribeLoggerConsumer (Class<?> caller, Level level) {

    this.level = level;

    logger = LoggerManager.getLogger((caller == null) ? ScribeLoggerConsumer.class : caller);
  }

  @Override
  public void accept (String message) {

    logger.log(level, message);
  }
}
