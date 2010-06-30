package org.smallmind.persistence.orm.hibernate;

/**
 * Bizzy Inc. (c) 2009
 * User: Arthur Svider
 * Date: Sep 11, 2009
 * Time: 5:33:58 PM
 */
public class DataPopulationException extends RuntimeException {

   public DataPopulationException () {

      super();
   }

   public DataPopulationException (String message) {

      super(message);
   }

   public DataPopulationException (String message, Throwable throwable) {

      super(message, throwable);
   }

   public DataPopulationException (Throwable throwable) {

      super(throwable);
   }

}
