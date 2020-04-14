/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Observable;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;

public class ObservableTracker {

  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
  private final ClaxonRegistry registry;

  public ObservableTracker (ClaxonRegistry registry) {

    this.registry = registry;
  }

  public <O extends Observable> O track (String identifier, MeterBuilder<?> builder, O observable, Tag... tags) {

    new RegisteredWeakReference<>(observable, referenceQueue, identifier, tags);

    observable.addObserver((Observable obs, Object arg) -> {

      Meter meter = registry.register(identifier, builder, tags);

      if (Number.class.isAssignableFrom(arg.getClass())) {
        meter.update(((Number)arg).longValue());
      } else if (String.class.equals(arg.getClass())) {
        meter.update(Long.parseLong((String)arg));
      } else {
        throw new IllegalArgumentException("Must be a long value(" + arg + ")");
      }
    });

    return observable;
  }

  public void sweep () {

    Reference<?> sweptReference;

    if ((sweptReference = referenceQueue.poll()) != null) {

      registry.unregister(((RegisteredWeakReference<?>)sweptReference).getIdentifier(), ((RegisteredWeakReference<?>)sweptReference).getTags());
    }
  }

  private static class RegisteredWeakReference<O extends Observable> extends WeakReference<O> {

    private String identifier;
    private Tag[] tags;

    public RegisteredWeakReference (O referent, ReferenceQueue<? super O> q, String identifier, Tag... tags) {

      super(referent, q);

      this.identifier = identifier;
      this.tags = tags;
    }

    public String getIdentifier () {

      return identifier;
    }

    public Tag[] getTags () {

      return tags;
    }
  }
}