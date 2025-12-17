/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.scribe.ink.indigenous;

import org.smallmind.scribe.pen.DefaultLoggerContext;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprint;

/**
 * Logging blueprint for the built-in indigenous logging implementation.
 * Provides adapters and error record construction compatible with the scribe core APIs.
 */
public class IndigenousLoggingBlueprint extends LoggingBlueprint<IndigenousRecord> {

  /**
   * Creates a logger adapter for the specified logger name.
   *
   * @param name the logger name
   * @return an adapter that emits indigenous records
   */
  @Override
  public LoggerAdapter getLoggingAdapter (String name) {

    return new IndigenousLoggerAdapter(name);
  }

  /**
   * Builds an error {@link Record} for the indigenous logger, enriching it with context.
   *
   * @param loggerName the originating logger name
   * @param throwable  the throwable to attach
   * @param message    formatted message string
   * @param args       substitution arguments
   * @return a fully constructed record suitable for downstream appenders
   */
  @Override
  public Record<IndigenousRecord> errorRecord (String loggerName, Throwable throwable, String message, Object... args) {

    IndigenousRecord indigenousRecord;
    LoggerContext loggerContext;

    indigenousRecord = new IndigenousRecord(loggerName, Level.FATAL, throwable, message, args);
    loggerContext = new DefaultLoggerContext();
    loggerContext.fillIn();
    indigenousRecord.setLoggerContext(loggerContext);

    return indigenousRecord.getRecord();
  }
}
