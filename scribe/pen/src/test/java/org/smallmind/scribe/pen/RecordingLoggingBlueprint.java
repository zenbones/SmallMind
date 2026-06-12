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
package org.smallmind.scribe.pen;

import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprint;

/**
 * The sole {@link LoggingBlueprint} on the test classpath (registered via the
 * {@code META-INF/services} test resource), letting {@code LoggingBlueprintFactory} resolve exactly
 * one provider so that {@link Logger}, {@link LoggerManager}, and {@link DefaultErrorHandler} are
 * exercisable without pulling an ink module into the reactor. Hands out {@link RecordingLoggerAdapter}
 * instances and builds a plain {@link RecordFixture} as its error record.
 */
public class RecordingLoggingBlueprint extends LoggingBlueprint<Object> {

  @Override
  public LoggerAdapter getLoggingAdapter (String name) {

    return new RecordingLoggerAdapter(name);
  }

  @Override
  public Record<Object> errorRecord (String loggerName, Throwable throwable, String message, Object... args) {

    return new RecordFixture().setLoggerName(loggerName).setLevel(Level.FATAL).setThrown(throwable).setMessage(MessageTranslator.translateMessage(message, args));
  }
}
