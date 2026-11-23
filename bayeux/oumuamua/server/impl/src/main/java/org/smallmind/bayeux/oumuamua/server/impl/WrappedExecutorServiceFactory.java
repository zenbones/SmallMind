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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorServiceFactory} that hands the server an executor service built elsewhere, typically a bean
 * defined by the container. It is a plain JavaBean with a no-argument constructor, so it can be declared and wired
 * through {@link #setExecutorService} without code. {@link #instantiate()} returns the assigned instance on every
 * call, and returns {@code null} if none has been assigned, in which case the server falls back to its default
 * executor. Ownership passes to the server along with the executor: {@link OumuamuaServer#stop()} shuts it down, so it must not be
 * shared with code that expects it to keep running, and a configuration using this factory should back only one
 * server.
 */
public class WrappedExecutorServiceFactory implements ExecutorServiceFactory {

  private ExecutorService executorService;

  /**
   * Creates a factory with no executor service; one should be assigned through {@link #setExecutorService} before
   * the server is constructed, or the server uses its default executor.
   */
  public WrappedExecutorServiceFactory () {

  }

  /**
   * @param executorService the executor service to hand to the server; owned and shut down by the server once
   *                        it is constructed
   */
  public void setExecutorService (ExecutorService executorService) {

    this.executorService = executorService;
  }

  /**
   * @return the executor service assigned through {@link #setExecutorService}, or {@code null} if none
   */
  @Override
  public ExecutorService instantiate () {

    return executorService;
  }
}
