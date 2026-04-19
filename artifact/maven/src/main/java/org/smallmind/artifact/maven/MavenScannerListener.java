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
package org.smallmind.artifact.maven;

import java.util.EventListener;

/**
 * Observer interface for receiving artifact-change notifications from a {@link MavenScanner}.
 *
 * <p>Implementations are registered via {@link MavenScanner#addMavenScannerListener} and
 * removed via {@link MavenScanner#removeMavenScannerListener}.  The callback is invoked
 * on the scanner's internal worker thread, so implementations must be thread-safe or must
 * marshal work to an appropriate execution context.
 *
 * <p>The callback is only invoked when at least one monitored coordinate has changed since
 * the previous scan cycle; scans that detect no changes produce no notification.
 */
public interface MavenScannerListener extends EventListener {

  /**
   * Called when one or more monitored artifacts have changed since the last scan.
   *
   * <p>The supplied event contains the full delta map (new artifact → prior artifact),
   * the current artifact array in coordinate order, and a {@link ClassLoader} that can
   * load all newly resolved artifacts and their transitive compile dependencies.
   *
   * @param event event describing which artifacts changed and providing the updated class loader;
   *              never {@code null}
   */
  void artifactChange (MavenScannerEvent event);
}
