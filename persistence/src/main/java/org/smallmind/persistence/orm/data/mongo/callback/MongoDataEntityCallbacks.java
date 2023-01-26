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
package org.smallmind.persistence.orm.data.mongo.callback;

import java.util.HashMap;
import java.util.LinkedList;
import org.bson.Document;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveCallback;

public class MongoDataEntityCallbacks implements EntityCallbacks {

  private final HashMap<CallbackType, LinkedList<MongoDataEntityCallback<?>>> callbackMap = new HashMap<>();

  public MongoDataEntityCallbacks () {

  }

  public MongoDataEntityCallbacks (MongoDataEntityCallback[] callbacks) {

    for (MongoDataEntityCallback<?> callback : callbacks) {
      addEntityCallback(callback);
    }
  }

  @Override
  public void addEntityCallback (EntityCallback<?> callback) {

    if (!MongoDataEntityCallback.class.isAssignableFrom(callback.getClass())) {
      throw new UnsupportedOperationException("Attempt to add an unsupported callback(" + callback.getClass() + ")");
    } else {

      CallbackType callbackType;
      LinkedList<MongoDataEntityCallback<?>> callbackList;

      if ((callbackList = callbackMap.get(callbackType = ((MongoDataEntityCallback<?>)callback).getCallbackType())) == null) {
        callbackMap.put(callbackType, callbackList = new LinkedList<>());
      }

      callbackList.add((MongoDataEntityCallback<?>)callback);
    }
  }

  @Override
  public <T> T callback (Class<? extends EntityCallback> callbackClass, T entity, Object... args) {

    CallbackType callbackType;

    if ((callbackType = CallbackType.from(callbackClass)) != null) {

      LinkedList<MongoDataEntityCallback<?>> callbackList;

      if (((callbackList = callbackMap.get(callbackType)) != null) && (!callbackList.isEmpty())) {
        for (MongoDataEntityCallback<?> callback : callbackList) {
          if (callback.getEntityClass().isAssignableFrom(entity.getClass())) {
            switch (callbackType) {
              case BEFORE_SAVE:
                entity = ((BeforeSaveCallback<T>)callback).onBeforeSave(entity, (Document)args[0], (String)args[1]);
                break;
              case AFTER_SAVE:
                entity = ((AfterSaveCallback<T>)callback).onAfterSave(entity, (Document)args[0], (String)args[1]);
                break;
              case BEFORE_CONVERT:
                entity = ((BeforeConvertCallback<T>)callback).onBeforeConvert(entity, (String)args[0]);
                break;
              case AFTER_CONVERT:
                entity = ((AfterConvertCallback<T>)callback).onAfterConvert(entity, (Document)args[0], (String)args[1]);
                break;
              default:
                throw new UnknownSwitchCaseException(callbackType.name());
            }
          }
        }
      }
    }

    return entity;
  }
}
