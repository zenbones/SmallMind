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
 * Holds the configuration defaults for a {@link ClaxonRegistry}, including the time source,
 * collection cadence, the bounded wait allowed for shutdown, optional features, default tags,
 * per-meter tags, and the strategy used to derive meter names from caller classes.
 *
 * <p>Instances may be constructed with all defaults via the no-argument constructor, or with
 * selective overrides via the full constructor. Individual properties can also be changed at
 * any time through the corresponding setter methods.
 */
public class ClaxonConfiguration {

  /**
   * Time source used by meters for wall-clock and monotonic readings.
   */
  private Clock clock = SystemClock.instance();

  /**
   * Interval between successive collection rounds.
   */
  private Stint collectionStint = new Stint(2, TimeUnit.SECONDS);

  /**
   * Maximum time {@link ClaxonRegistry#stop()} waits for the background collection worker to terminate;
   * may be {@code null}, in which case the registry falls back to five times the {@link #collectionStint}.
   */
  private Stint terminationStint = new Stint(10, TimeUnit.SECONDS);

  /**
   * Optional registry-level features that produce their own {@link Quantity} readings each collection round.
   */
  private Feature[] features;

  /**
   * Tags applied to every meter reading emitted by the registry.
   */
  private Tag[] registryTags = new Tag[0];

  /**
   * Per-meter tag overrides keyed by meter name; may be {@code null} when none are configured.
   */
  private Map<String, Tag[]> meterTags;

  /**
   * Strategy used to derive a string name from a caller class when registering a meter.
   */
  private NamingStrategy namingStrategy = new ImpliedNamingStrategy();

  /**
   * Creates a configuration with all properties set to their default values:
   * {@link SystemClock}, a two-second collection stint, a ten-second termination stint,
   * no features, no tags, and an {@link ImpliedNamingStrategy}.
   */
  public ClaxonConfiguration () {

  }

  /**
   * Creates a configuration that selectively overrides defaults. Any parameter that is
   * {@code null} leaves the corresponding default value in place.
   *
   * @param clock            time source for meter calculations; {@code null} retains {@link SystemClock}
   * @param collectionStint  interval between collection rounds; {@code null} retains the two-second default
   * @param terminationStint maximum time {@link ClaxonRegistry#stop()} waits for the collection worker to terminate; {@code null} retains the ten-second default
   * @param features         registry-level features to activate; {@code null} leaves features unset
   * @param registryTags     tags to attach to every meter reading; {@code null} leaves an empty array
   * @param meterTags        per-meter tag overrides keyed by meter name; {@code null} leaves the map unset
   * @param namingStrategy   strategy for deriving meter names; {@code null} retains {@link ImpliedNamingStrategy}
   */
  public ClaxonConfiguration (Clock clock, Stint collectionStint, Stint terminationStint, Feature[] features, Tag[] registryTags, Map<String, Tag[]> meterTags, NamingStrategy namingStrategy) {

    if (clock != null) {
      this.clock = clock;
    }
    if (collectionStint != null) {
      this.collectionStint = collectionStint;
    }
    if (terminationStint != null) {
      this.terminationStint = terminationStint;
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
   * Returns the time source used by meters for wall-clock and monotonic readings.
   *
   * @return the configured {@link Clock}
   */
  public Clock getClock () {

    return clock;
  }

  /**
   * Replaces the time source used by meters for wall-clock and monotonic readings.
   *
   * @param clock the new {@link Clock} to use
   */
  public void setClock (Clock clock) {

    this.clock = clock;
  }

  /**
   * Returns the interval between successive metric collection rounds.
   *
   * @return the configured collection {@link Stint}
   */
  public Stint getCollectionStint () {

    return collectionStint;
  }

  /**
   * Sets the interval between successive metric collection rounds.
   *
   * @param collectionStint the new collection {@link Stint}
   */
  public void setCollectionStint (Stint collectionStint) {

    this.collectionStint = collectionStint;
  }

  /**
   * Returns the maximum time {@link ClaxonRegistry#stop()} waits for the background collection worker
   * to terminate, or {@code null} when the registry should fall back to five times the collection stint.
   *
   * @return the configured termination {@link Stint}, or {@code null}
   */
  public Stint getTerminationStint () {

    return terminationStint;
  }

  /**
   * Returns the registry-level features that produce additional {@link Quantity} readings
   * on each collection round, or {@code null} if none are configured.
   *
   * @return the configured {@link Feature} array, or {@code null}
   */
  public Feature[] getFeatures () {

    return features;
  }

  /**
   * Sets the registry-level features that produce additional {@link Quantity} readings
   * on each collection round.
   *
   * @param features the features to activate
   */
  public void setFeatures (Feature[] features) {

    this.features = features;
  }

  /**
   * Returns the tags that are appended to every meter reading emitted by the registry.
   * The array is never {@code null}; it may be empty.
   *
   * @return the registry-wide tag array
   */
  public Tag[] getRegistryTags () {

    return registryTags;
  }

  /**
   * Sets the tags that are appended to every meter reading emitted by the registry.
   *
   * @param registryTags the new registry-wide tags
   */
  public void setRegistryTags (Tag[] registryTags) {

    this.registryTags = registryTags;
  }

  /**
   * Returns the strategy used to derive a string name from a caller class when a meter
   * is registered.
   *
   * @return the configured {@link NamingStrategy}
   */
  public NamingStrategy getNamingStrategy () {

    return namingStrategy;
  }

  /**
   * Sets the strategy used to derive a string name from a caller class when a meter
   * is registered.
   *
   * @param namingStrategy the new {@link NamingStrategy}
   */
  public void setNamingStrategy (NamingStrategy namingStrategy) {

    this.namingStrategy = namingStrategy;
  }

  /**
   * Sets per-meter tag overrides. The map keys are meter names; the values are arrays of
   * {@link Tag} instances that will be merged with the registry-wide tags and any
   * instance-level tags when calculating the effective tag set for that meter.
   *
   * @param meterTags map of meter name to tag array
   */
  public void setMeterTags (HashMap<String, Tag[]> meterTags) {

    this.meterTags = meterTags;
  }

  /**
   * Returns the tags configured specifically for the named meter, or {@code null} when
   * no per-meter tags are defined for that name.
   *
   * @param name meter name to look up
   * @return the meter-specific {@link Tag} array, or {@code null}
   */
  public Tag[] forMeter (String name) {

    return (meterTags == null) ? null : meterTags.get(name);
  }

  /**
   * Builds the effective tag array for a named meter by merging, in order:
   * registry-wide tags, per-meter tags, and any instance-specific tags supplied by the caller.
   *
   * @param name         the meter name used to retrieve per-meter tags
   * @param instanceTags instance-specific tags provided at the call site; may be empty or {@code null}
   * @return a merged {@link Tag} array, or {@code null} when no tags exist at any level
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
