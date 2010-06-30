package org.smallmind.persistence.statistics;

public interface StatisticsFactory {

   public abstract boolean isEnabled ();

   public abstract void setEnabled (boolean enabled);

   public abstract Statistics getStatistics ();

   public abstract Statistics removeStatistics ();
}
