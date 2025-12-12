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
package org.smallmind.claxon.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.feature.Feature;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * Encapsulates registry configuration defaults such as timing, tags, and naming strategies.
 * Instances can be built manually or populated via configuration to customize how the registry operates.
 */
public class ClaxonConfiguration {

  private Clock clock = SystemClock.instance();
  private Stint collectionStint = new Stint(2, TimeUnit.SECONDS);
  private Feature[] features;
  private Tag[] registryTags = new Tag[0];
  private Map<String, Tag[]> meterTags;
  private NamingStrategy namingStrategy = new ImpliedNamingStrategy();

  /**
   * Creates a configuration with default values.
   */
  public ClaxonConfiguration () {

  }

  /**
   * Creates a configuration overriding selected defaults.
   *
   * @param clock           the clock to use for timing calculations, defaults to {@link SystemClock} when {@code null}
   * @param collectionStint cadence for collection, defaults to two seconds when {@code null}
   * @param features        optional registry features to enable
   * @param registryTags    default tags applied to every meter
   * @param meterTags       per-meter tags keyed by meter name
   * @param namingStrategy  strategy to use when generating meter names, defaults to {@link ImpliedNamingStrategy}
   */
  public ClaxonConfiguration (Clock clock, Stint collectionStint, Feature[] features, Tag[] registryTags, Map<String, Tag[]> meterTags, NamingStrategy namingStrategy) {

    if (clock != null) {
      this.clock = clock;
    }
    if (collectionStint != null) {
      this.collectionStint = collectionStint;
    }
    if (features != null) {
      this.features = features;
    }
    if (registryTags != null) {
      this.registryTags = registryTags;
    }
    if (meterTags != null) {
      this.meterTags = meterTags;
    }
    if (namingStrategy != null) {
      this.namingStrategy = namingStrategy;
    }
  }

  /**
   * Returns the clock used for timing calculations.
   *
   * @return the configured {@link Clock}
   */
  public Clock getClock () {

    return clock;
  }

  /**
   * Replaces the clock used for timing calculations.
   *
   * @param clock the new {@link Clock}
   */
  public void setClock (Clock clock) {

    this.clock = clock;
  }

  /**
   * Returns the collection cadence for periodic emitters.
   *
   * @return the configured {@link Stint}
   */
  public Stint getCollectionStint () {

    return collectionStint;
  }

  /**
   * Sets the collection cadence for periodic emitters.
   *
   * @param collectionStint the new {@link Stint}
   */
  public void setCollectionStint (Stint collectionStint) {

    this.collectionStint = collectionStint;
  }

  /**
   * Returns any optional features configured for the registry.
   *
   * @return feature array or {@code null}
   */
  public Feature[] getFeatures () {

    return features;
  }

  /**
   * Sets optional features to enable in the registry.
   *
   * @param features the features to enable
   */
  public void setFeatures (Feature[] features) {

    this.features = features;
  }

  /**
   * Returns tags applied to every meter registered with the registry.
   *
   * @return array of default tags, possibly empty
   */
  public Tag[] getRegistryTags () {

    return registryTags;
  }

  /**
   * Sets tags applied to every meter registered with the registry.
   *
   * @param registryTags default registry tags
   */
  public void setRegistryTags (Tag[] registryTags) {

    this.registryTags = registryTags;
  }

  /**
   * Returns the naming strategy used to derive meter identifiers.
   *
   * @return the configured {@link NamingStrategy}
   */
  public NamingStrategy getNamingStrategy () {

    return namingStrategy;
  }

  /**
   * Sets the naming strategy used to derive meter identifiers.
   *
   * @param namingStrategy the new strategy
   */
  public void setNamingStrategy (NamingStrategy namingStrategy) {

    this.namingStrategy = namingStrategy;
  }

  /**
   * Sets meter-specific tags keyed by meter name.
   *
   * @param meterTags per-meter tags
   */
  public void setMeterTags (HashMap<String, Tag[]> meterTags) {

    this.meterTags = meterTags;
  }

  /**
   * Looks up tags configured for a specific meter name.
   *
   * @param name meter name
   * @return tags for the meter or {@code null} when none are defined
   */
  public Tag[] forMeter (String name) {

    return (meterTags == null) ? null : meterTags.get(name);
  }

  /**
   * Combines registry-level, meter-level, and instance-level tags for a particular meter.
   *
   * @param name         meter name
   * @param instanceTags instance-specific tags provided by the caller
   * @return merged tags array or {@code null} if no tags exist
   */
  public Tag[] calculateTags (String name, Tag... instanceTags) {

    Tag[] tagsForMeter = forMeter(name);
    int tagCount = ((registryTags == null) ? 0 : registryTags.length) + ((tagsForMeter == null) ? 0 : tagsForMeter.length) + ((instanceTags == null) ? 0 : instanceTags.length);

    if (tagCount == 0) {

      return null;
    } else {

      Tag[] mergedTags = new Tag[tagCount];
      int index = 0;

      if ((registryTags != null) && (registryTags.length > 0)) {
        System.arraycopy(registryTags, 0, mergedTags, 0, registryTags.length);
        index += registryTags.length;
      }
      if ((tagsForMeter != null) && (tagsForMeter.length > 0)) {
        System.arraycopy(tagsForMeter, 0, mergedTags, index, tagsForMeter.length);
        index += tagsForMeter.length;
      }
      if ((instanceTags != null) && (instanceTags.length > 0)) {
        System.arraycopy(instanceTags, 0, mergedTags, index, instanceTags.length);
      }

      return mergedTags;
    }
  }
}
