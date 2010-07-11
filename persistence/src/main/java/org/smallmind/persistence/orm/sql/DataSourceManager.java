package org.smallmind.persistence.orm.sql;

import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.smallmind.persistence.orm.ORMInitializationException;

public class DataSourceManager {

   private static final ConcurrentHashMap<String, DataSource> SOURCE_MAP = new ConcurrentHashMap<String, DataSource>();

   public static void registerDataSource (String key, DataSource dataSource) {

      SOURCE_MAP.put(key, dataSource);
   }

   public static DataSource getDataSource (String key) {

      DataSource dataSource;

      if ((dataSource = SOURCE_MAP.get(key)) == null) {
         throw new ORMInitializationException("No DataSOurce was mapped to the key(%s)", key);
      }

      return dataSource;
   }
}
