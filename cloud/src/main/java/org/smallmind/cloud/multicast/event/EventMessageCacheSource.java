package org.smallmind.cloud.multicast.event;

import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.indigenous.CacheReference;
import org.smallmind.quorum.cache.indigenous.CacheSource;

public class EventMessageCacheSource implements CacheSource<EventMessageKey, EventMessageMold, EventMessageCacheEntry> {

   public EventMessageCacheEntry createEntry (EventMessageKey key, Object... parameters)
      throws CacheException {

      return new EventMessageCacheEntry(new EventMessageMold());
   }

   public CacheReference<EventMessageKey, EventMessageCacheEntry> wrapReference (EventMessageKey key, EventMessageMold value)
      throws CacheException {

      return new CacheReference<EventMessageKey, EventMessageCacheEntry>(key, new EventMessageCacheEntry(new EventMessageMold()));
   }
}
