package dev.js.mtrheadlight.bind;

import mtr.data.TicketSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public final class MtrBalance {
   private static final String OBJECTIVE = "mtr_balance";

   private MtrBalance() {
   }

   public static int get(Level world, Player player) {
      if (world != null && player != null) {
         try {
            Scoreboard scoreboard = world.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName());
            if (objective == null) {
               return 0;
            } else {
               Score score = scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), objective);
               return score == null ? 0 : score.getScore();
            }
         } catch (Throwable var5) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean set(Level world, Player player, int balance) {
      if (world != null && player != null) {
         try {
            TicketSystem.addObjectivesIfMissing(world);
            Scoreboard scoreboard = world.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName());
            if (objective == null) {
               return false;
            } else {
               scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), objective).setScore(balance);
               return true;
            }
         } catch (Throwable var5) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int add(Level world, Player player, int delta) {
      int before = get(world, player);
      int after = before + delta;
      return set(world, player, after) ? after : before;
   }

   private static String objectiveName() {
      try {
         return "mtr_balance";
      } catch (Throwable var1) {
         return "mtr_balance";
      }
   }
}
