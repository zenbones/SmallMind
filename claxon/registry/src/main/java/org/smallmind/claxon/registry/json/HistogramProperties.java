/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.claxon.registry.json;

import org.smallmind.claxon.registry.Percentile;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;

@Doppelganger(namespace = "http://org.smallmind/claxon/registry")
public class HistogramProperties {

  @View(adapter = StintXmlAdapter.class, idioms = @Idiom(visibility = IN))
  private Stint resolutionStint;
  @View(idioms = @Idiom(visibility = IN))
  private Percentile[] percentiles;
  @View(idioms = @Idiom(visibility = IN))
  private Long lowestDiscernibleValue;
  @View(idioms = @Idiom(visibility = IN))
  private Long highestTrackableValue;
  @View(idioms = @Idiom(visibility = IN))
  private Integer numberOfSignificantValueDigits;

  public Stint getResolutionStint () {

    return resolutionStint;
  }

  public void setResolutionStint (Stint resolutionStint) {

    this.resolutionStint = resolutionStint;
  }

  public Percentile[] getPercentiles () {

    return percentiles;
  }

  public void setPercentiles (Percentile[] percentiles) {

    this.percentiles = percentiles;
  }

  public Long getLowestDiscernibleValue () {

    return lowestDiscernibleValue;
  }

  public void setLowestDiscernibleValue (Long lowestDiscernibleValue) {

    this.lowestDiscernibleValue = lowestDiscernibleValue;
  }

  public Long getHighestTrackableValue () {

    return highestTrackableValue;
  }

  public void setHighestTrackableValue (Long highestTrackableValue) {

    this.highestTrackableValue = highestTrackableValue;
  }

  public Integer getNumberOfSignificantValueDigits () {

    return numberOfSignificantValueDigits;
  }

  public void setNumberOfSignificantValueDigits (Integer numberOfSignificantValueDigits) {

    this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
  }
}
