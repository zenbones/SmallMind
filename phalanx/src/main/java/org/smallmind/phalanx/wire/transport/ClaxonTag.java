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
package org.smallmind.phalanx.wire.transport;

/**
 * Enumeration of Claxon instrumentation tags emitted at key points in the wire transport pipeline.
 * Each constant carries a human-readable display string used when recording measurements.
 */
public enum ClaxonTag {

  /**
   * Recorded when a request transport instance is acquired from the pool or registry.
   */
  ACQUIRE_REQUEST_TRANSPORT("Acquire Request Transport"),

  /**
   * Recorded when a transport message payload is constructed prior to sending.
   */
  CONSTRUCT_MESSAGE("Construct Message"),

  /**
   * Recorded to capture elapsed time while a request is in transit to the responder.
   */
  REQUEST_TRANSIT_TIME("Request Transit Time"),

  /**
   * Recorded to capture elapsed time while a response travels back to the caller.
   */
  RESPONSE_TRANSIT_TIME("Response Transit Time"),

  /**
   * Recorded when a pending response callback is completed by an inbound result signal.
   */
  COMPLETE_CALLBACK("Complete Callback"),

  /**
   * Recorded to measure time spent waiting for a result to be delivered to the caller.
   */
  ACQUIRE_RESULT("Acquire Result");

  private final String display;

  /**
   * Constructs the enum constant with its display string.
   *
   * @param display human-readable label emitted with measurements
   */
  ClaxonTag (String display) {

    this.display = display;
  }

  /**
   * Returns the human-readable display string for this tag.
   *
   * @return display label suitable for metrics output
   */
  public String getDisplay () {

    return display;
  }
}
