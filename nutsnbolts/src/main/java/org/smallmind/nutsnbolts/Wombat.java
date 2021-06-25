package org.smallmind.nutsnbolts;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Wombat {

  public static void main (String... args) {

    Map<Integer, Stats[]> statsMap = Map.of(
      60, new Stats[] {new NormalStats(60), new PreciseStats(60), new PowerfulStats(60), new ExtraPowerfulStats(60)},
      70, new Stats[] {new NormalStats(70), new PreciseStats(70), new PowerfulStats(70), new ExtraPowerfulStats(70)},
      80, new Stats[] {new NormalStats(80), new PreciseStats(80), new PowerfulStats(80), new ExtraPowerfulStats(80)}
    );

    for (int index = 0; index < 10; index++) {

      int roll1 = ThreadLocalRandom.current().nextInt(0, 10);
      int roll2 = ThreadLocalRandom.current().nextInt(0, 10);

      for (Stats[] statsArray : statsMap.values()) {
        for (Stats stats : statsArray) {
          stats.add(roll1, roll2);
        }
      }
    }

    for (Map.Entry<Integer, Stats[]> entry : statsMap.entrySet()) {
      System.out.println(entry.getKey() + " ==================================");
      for (Stats stats : entry.getValue()) {
        System.out.println(stats.getName() + " ------------------------");
        if (stats instanceof PreciseStats) {

          double preciseHitPercentage = round(stats.getHitRolls() / (stats.getTotalRolls() * 1.0D) * 100);
          double normalHitPercentage = round(((PreciseStats)stats).getNormalHits() / (stats.getTotalRolls() * 1.0D) * 100);

          System.out.println("hits: " + preciseHitPercentage + "% (+" + (preciseHitPercentage - normalHitPercentage) + "%)");
        } else {
          System.out.println("hits: " + round((stats.getHitRolls() / (stats.getTotalRolls() * 1.0D) * 100)) + "%");
        }
        System.out.println("damage per roll: " + (stats.getTotalDamage() / stats.getTotalRolls()));
        System.out.println("damage per hit: " + (stats.getTotalDamage() / stats.getHitRolls()));
      }
    }

    System.out.println("done...");
  }

  public static double round (double percentage) {

    return Math.round(percentage * 100) / 100.0D;
  }

  public static int asPercentile (int die1, int die2) {

    int total = (die1 * 10) + die2;

    return (total == 0) ? 100 : total;
  }

  private static class NormalStats extends Stats {

    public NormalStats (int percent) {

      super(percent);
    }

    @Override
    public String getName () {

      return "Normal";
    }

    @Override
    public int calculateDamage (int roll1, int roll2) {

      int percentage;

      return ((percentage = asPercentile(roll1, roll2)) <= getPercent()) ? percentage : 0;
    }
  }

  private static class PreciseStats extends Stats {

    private int normalHits;

    public PreciseStats (int percent) {

      super(percent);
    }

    @Override
    public String getName () {

      return "Precise";
    }

    public int getNormalHits () {

      return normalHits;
    }

    @Override
    public int calculateDamage (int roll1, int roll2) {

      int percentage1 = asPercentile(roll1, roll2);
      int percentage2 = asPercentile(roll2, roll1);
      int damage1 = (percentage1 <= getPercent()) ? percentage1 : 0;
      int damage2 = (percentage2 <= getPercent()) ? percentage2 : 0;

      if (percentage1 <= getPercent()) {
        normalHits++;
      }

      return Math.max(damage1, damage2);
    }
  }

  private static class PowerfulStats extends Stats {

    public PowerfulStats (int percent) {

      super(percent);
    }

    @Override
    public String getName () {

      return "Powerful";
    }

    @Override
    public int calculateDamage (int roll1, int roll2) {

      int percentage;

      return ((percentage = asPercentile(roll1, roll2)) <= getPercent()) ? percentage + 20 : 0;
    }
  }

  private static class ExtraPowerfulStats extends Stats {

    public ExtraPowerfulStats (int percent) {

      super(percent);
    }

    @Override
    public String getName () {

      return "ExtraPowerful";
    }

    @Override
    public int calculateDamage (int roll1, int roll2) {

      int percentage;

      if ((percentage = asPercentile(roll1, roll2)) <= getPercent()) {

        int bonus1 = ThreadLocalRandom.current().nextInt(0, 2) == 1 ? 10 : 0;
        int bonus2 = ThreadLocalRandom.current().nextInt(0, 2) == 1 ? 10 : 0;

        return percentage + 20 + bonus1 + bonus2;
      } else {

        return 0;
      }
    }
  }

  private abstract static class Stats {

    private final int percent;
    private int hitRolls;
    private int totalRolls;
    private int totalDamage;

    public Stats (int percent) {

      this.percent = percent;
    }

    public abstract String getName ();

    public abstract int calculateDamage (int roll1, int roll2);

    public int getPercent () {

      return percent;
    }

    public int getHitRolls () {

      return hitRolls;
    }

    public int getTotalRolls () {

      return totalRolls;
    }

    public int getTotalDamage () {

      return totalDamage;
    }

    public void add (int roll1, int roll2) {

      int damage;

      totalRolls++;

      if ((damage = calculateDamage(roll1, roll2)) > 0) {
        hitRolls++;
        totalDamage += damage;
      }
    }
  }
}
