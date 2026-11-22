package dev.js.mtrheadlight.xiamen.drinks;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

public final class DrinksMachinePotions {
   private DrinksMachinePotions() {
   }

   public static enum Preset {
      MIXED("mixed", "message.mtrlte.drinks_machine.preset.mixed", Potions.LONG_SWIFTNESS, Potions.LONG_REGENERATION, Potions.LONG_FIRE_RESISTANCE, Potions.LONG_LEAPING, Potions.LONG_NIGHT_VISION, Potions.LONG_WATER_BREATHING, Potions.HEALING),
      VITALITY("vitality", "message.mtrlte.drinks_machine.preset.vitality", Potions.STRONG_HEALING, Potions.LONG_REGENERATION, Potions.LONG_FIRE_RESISTANCE, Potions.LONG_LEAPING, Potions.HEALING),
      NIGHTLY("nightly", "message.mtrlte.drinks_machine.preset.nightly", Potions.LONG_NIGHT_VISION, Potions.LONG_WATER_BREATHING, Potions.LONG_SWIFTNESS, Potions.LONG_LEAPING, Potions.LONG_SLOW_FALLING),
      STRENGTH("strength", "message.mtrlte.drinks_machine.preset.strength", Potions.STRONG_STRENGTH, Potions.LONG_SWIFTNESS, Potions.LONG_FIRE_RESISTANCE, Potions.LONG_REGENERATION, Potions.HEALING);

      private final String id;
      private final String translationKey;
      private final List<Potion> potions;

      private Preset(final String id, final String translationKey, final Potion... potions) {
         this.id = id;
         this.translationKey = translationKey;
         this.potions = List.of(potions);
      }

      public String id() {
         return this.id;
      }

      public Component displayName() {
         return Component.translatable(this.translationKey);
      }

      public List<Potion> potions() {
         return this.potions;
      }

      public DrinksMachinePotions.Preset next() {
         DrinksMachinePotions.Preset[] all = values();
         return all[(this.ordinal() + 1) % all.length];
      }

      public Potion pick(RandomSource random) {
         return (Potion)this.potions.get(random.nextInt(this.potions.size()));
      }

      public static DrinksMachinePotions.Preset byId(String id) {
         for(DrinksMachinePotions.Preset preset : values()) {
            if (preset.id.equals(id)) {
               return preset;
            }
         }

         return MIXED;
      }
   }
}
