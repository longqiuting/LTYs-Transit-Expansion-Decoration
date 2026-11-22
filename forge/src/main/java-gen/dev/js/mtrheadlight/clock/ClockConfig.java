package dev.js.mtrheadlight.clock;

import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public final class ClockConfig {
   public static final float MIN_LENGTH = 0.1F;
   public static final float MAX_LENGTH = 50.0F;
   public static final float LENGTH_STEP = 0.1F;
   public static final float DEFAULT_FACE_RADIUS = 0.36F;
   public static final float DEFAULT_TICK_RADIUS = 0.33F;
   public static final String DEFAULT_SOUND = "minecraft:block.bell.use";
   public static final double MAX_OFFSET_MINUTES = 1440.0D;
   public ClockConfig.TimeSource timeSource = ClockConfig.TimeSource.MC;
   public ClockConfig.GlowMode glow = ClockConfig.GlowMode.OFF;
   public float hourLength = 0.2F;
   public float minuteLength = 0.28F;
   public float secondLength = 0.33F;
   public float faceRadius = 0.36F;
   public float tickRadius = 0.33F;
   public boolean showFace = true;
   public boolean showTicks = true;
   public boolean showHour = true;
   public boolean showMinute = true;
   public boolean showSecond = true;
   public boolean showHub = true;
   public int faceColor = 15919832;
   public int hourColor = 1776411;
   public int minuteColor = 1776411;
   public int secondColor = 12597547;
   public int hubColor = 1776411;
   public int tickColor = 2763306;
   public boolean chime = false;
   public String chimeSound = "minecraft:block.bell.use";
   public ClockConfig.ChimeMode chimeMode = ClockConfig.ChimeMode.SINGLE;
   public ClockConfig.TickStyle tickStyle = ClockConfig.TickStyle.BAR;
   public boolean redstone = false;
   public double offsetMinutes = 0.0D;

   public ClockConfig copy() {
      ClockConfig out = new ClockConfig();
      out.timeSource = this.timeSource;
      out.glow = this.glow;
      out.hourLength = this.hourLength;
      out.minuteLength = this.minuteLength;
      out.secondLength = this.secondLength;
      out.faceRadius = this.faceRadius;
      out.tickRadius = this.tickRadius;
      out.showFace = this.showFace;
      out.showTicks = this.showTicks;
      out.showHour = this.showHour;
      out.showMinute = this.showMinute;
      out.showSecond = this.showSecond;
      out.showHub = this.showHub;
      out.faceColor = this.faceColor;
      out.hourColor = this.hourColor;
      out.minuteColor = this.minuteColor;
      out.secondColor = this.secondColor;
      out.hubColor = this.hubColor;
      out.tickColor = this.tickColor;
      out.chime = this.chime;
      out.chimeSound = this.chimeSound;
      out.chimeMode = this.chimeMode;
      out.tickStyle = this.tickStyle;
      out.redstone = this.redstone;
      out.offsetMinutes = this.offsetMinutes;
      return out;
   }

   public void clamp() {
      if (this.timeSource == null) {
         this.timeSource = ClockConfig.TimeSource.MC;
      }

      if (this.glow == null) {
         this.glow = ClockConfig.GlowMode.OFF;
      }

      if (this.chimeMode == null) {
         this.chimeMode = ClockConfig.ChimeMode.SINGLE;
      }

      if (this.tickStyle == null) {
         this.tickStyle = ClockConfig.TickStyle.BAR;
      }

      this.hourLength = snapLength(this.hourLength, 0.2F);
      this.minuteLength = snapLength(this.minuteLength, 0.28F);
      this.secondLength = snapLength(this.secondLength, 0.33F);
      this.faceRadius = snapLength(this.faceRadius, 0.36F);
      this.tickRadius = snapLength(this.tickRadius, 0.33F);
      this.faceColor &= 16777215;
      this.hourColor &= 16777215;
      this.minuteColor &= 16777215;
      this.secondColor &= 16777215;
      this.hubColor &= 16777215;
      this.tickColor &= 16777215;
      this.chimeSound = normalizeSound(this.chimeSound);
      this.offsetMinutes = Math.max(-1440.0D, Math.min(1440.0D, this.offsetMinutes));
      if (Double.isNaN(this.offsetMinutes)) {
         this.offsetMinutes = 0.0D;
      }

   }

   public static float snapLength(float value, float fallback) {
      if (!Float.isNaN(value) && !Float.isInfinite(value)) {
         float v = Math.max(0.1F, Math.min(50.0F, value));
         v = (float)Math.round(v / 0.1F) * 0.1F;
         return Math.max(0.1F, Math.min(50.0F, v));
      } else {
         return fallback;
      }
   }

   public static String normalizeSound(String raw) {
      if (raw != null && !raw.isEmpty()) {
         ResourceLocation id = ResourceLocation.tryParse(raw.trim().toLowerCase(Locale.ROOT));
         return id == null ? "minecraft:block.bell.use" : id.toString();
      } else {
         return "minecraft:block.bell.use";
      }
   }

   public void writeNbt(CompoundTag nbt) {
      nbt.putString("timeSource", this.timeSource.getSerializedName());
      nbt.putString("glow", this.glow.getSerializedName());
      nbt.putFloat("hourLen", this.hourLength);
      nbt.putFloat("minuteLen", this.minuteLength);
      nbt.putFloat("secondLen", this.secondLength);
      nbt.putFloat("faceRadius", this.faceRadius);
      nbt.putFloat("tickRadius", this.tickRadius);
      nbt.putBoolean("showFace", this.showFace);
      nbt.putBoolean("showTicks", this.showTicks);
      nbt.putBoolean("showHour", this.showHour);
      nbt.putBoolean("showMinute", this.showMinute);
      nbt.putBoolean("showSecond", this.showSecond);
      nbt.putBoolean("showHub", this.showHub);
      nbt.putInt("faceColor", this.faceColor);
      nbt.putInt("hourColor", this.hourColor);
      nbt.putInt("minuteColor", this.minuteColor);
      nbt.putInt("secondColor", this.secondColor);
      nbt.putInt("hubColor", this.hubColor);
      nbt.putInt("tickColor", this.tickColor);
      nbt.putBoolean("chime", this.chime);
      nbt.putString("chimeSound", this.chimeSound);
      nbt.putString("chimeMode", this.chimeMode.getSerializedName());
      nbt.putString("tickStyle", this.tickStyle.getSerializedName());
      nbt.putBoolean("redstone", this.redstone);
      nbt.putDouble("offsetMinutes", this.offsetMinutes);
   }

   public void readNbt(CompoundTag nbt) {
      ClockConfig fallback = new ClockConfig();
      this.timeSource = nbt.contains("timeSource") ? ClockConfig.TimeSource.byName(nbt.getString("timeSource"), fallback.timeSource) : fallback.timeSource;
      this.glow = nbt.contains("glow") ? ClockConfig.GlowMode.byName(nbt.getString("glow"), fallback.glow) : fallback.glow;
      this.hourLength = nbt.contains("hourLen") ? nbt.getFloat("hourLen") : fallback.hourLength;
      this.minuteLength = nbt.contains("minuteLen") ? nbt.getFloat("minuteLen") : fallback.minuteLength;
      this.secondLength = nbt.contains("secondLen") ? nbt.getFloat("secondLen") : fallback.secondLength;
      float legacyFace = 1.08F * Math.max(this.hourLength, Math.max(this.minuteLength, this.secondLength));
      this.faceRadius = nbt.contains("faceRadius") ? nbt.getFloat("faceRadius") : legacyFace;
      this.tickRadius = nbt.contains("tickRadius") ? nbt.getFloat("tickRadius") : legacyFace * 0.92F;
      this.showFace = !nbt.contains("showFace") || nbt.getBoolean("showFace");
      this.showTicks = !nbt.contains("showTicks") || nbt.getBoolean("showTicks");
      this.showHour = !nbt.contains("showHour") || nbt.getBoolean("showHour");
      this.showMinute = !nbt.contains("showMinute") || nbt.getBoolean("showMinute");
      this.showSecond = !nbt.contains("showSecond") || nbt.getBoolean("showSecond");
      this.showHub = !nbt.contains("showHub") || nbt.getBoolean("showHub");
      this.faceColor = nbt.contains("faceColor") ? nbt.getInt("faceColor") : fallback.faceColor;
      this.hourColor = nbt.contains("hourColor") ? nbt.getInt("hourColor") : fallback.hourColor;
      this.minuteColor = nbt.contains("minuteColor") ? nbt.getInt("minuteColor") : fallback.minuteColor;
      this.secondColor = nbt.contains("secondColor") ? nbt.getInt("secondColor") : fallback.secondColor;
      this.hubColor = nbt.contains("hubColor") ? nbt.getInt("hubColor") : fallback.hubColor;
      this.tickColor = nbt.contains("tickColor") ? nbt.getInt("tickColor") : fallback.tickColor;
      this.chime = nbt.contains("chime") ? nbt.getBoolean("chime") : fallback.chime;
      this.chimeSound = nbt.contains("chimeSound") ? nbt.getString("chimeSound") : fallback.chimeSound;
      this.chimeMode = nbt.contains("chimeMode") ? ClockConfig.ChimeMode.byName(nbt.getString("chimeMode"), fallback.chimeMode) : fallback.chimeMode;
      this.tickStyle = nbt.contains("tickStyle") ? ClockConfig.TickStyle.byName(nbt.getString("tickStyle"), fallback.tickStyle) : fallback.tickStyle;
      this.redstone = nbt.contains("redstone") ? nbt.getBoolean("redstone") : fallback.redstone;
      this.offsetMinutes = nbt.contains("offsetMinutes") ? nbt.getDouble("offsetMinutes") : fallback.offsetMinutes;
      this.clamp();
   }

   public void writeBuf(FriendlyByteBuf buf) {
      buf.writeUtf(this.timeSource.getSerializedName());
      buf.writeUtf(this.glow.getSerializedName());
      buf.writeFloat(this.hourLength);
      buf.writeFloat(this.minuteLength);
      buf.writeFloat(this.secondLength);
      buf.writeFloat(this.faceRadius);
      buf.writeFloat(this.tickRadius);
      buf.writeBoolean(this.showFace);
      buf.writeBoolean(this.showHour);
      buf.writeBoolean(this.showMinute);
      buf.writeBoolean(this.showSecond);
      buf.writeBoolean(this.showHub);
      buf.writeBoolean(this.showTicks);
      buf.writeInt(this.faceColor);
      buf.writeInt(this.hourColor);
      buf.writeInt(this.minuteColor);
      buf.writeInt(this.secondColor);
      buf.writeInt(this.hubColor);
      buf.writeInt(this.tickColor);
      buf.writeBoolean(this.chime);
      buf.writeUtf(this.chimeSound);
      buf.writeUtf(this.chimeMode.getSerializedName());
      buf.writeUtf(this.tickStyle.getSerializedName());
      buf.writeBoolean(this.redstone);
      buf.writeDouble(this.offsetMinutes);
   }

   public static ClockConfig readBuf(FriendlyByteBuf buf) {
      ClockConfig cfg = new ClockConfig();
      cfg.timeSource = ClockConfig.TimeSource.byName(buf.readUtf(16), cfg.timeSource);
      cfg.glow = ClockConfig.GlowMode.byName(buf.readUtf(16), cfg.glow);
      cfg.hourLength = buf.readFloat();
      cfg.minuteLength = buf.readFloat();
      cfg.secondLength = buf.readFloat();
      cfg.faceRadius = buf.readFloat();
      cfg.tickRadius = buf.readFloat();
      cfg.showFace = buf.readBoolean();
      cfg.showHour = buf.readBoolean();
      cfg.showMinute = buf.readBoolean();
      cfg.showSecond = buf.readBoolean();
      cfg.showHub = buf.readBoolean();
      cfg.showTicks = buf.readBoolean();
      cfg.faceColor = buf.readInt();
      cfg.hourColor = buf.readInt();
      cfg.minuteColor = buf.readInt();
      cfg.secondColor = buf.readInt();
      cfg.hubColor = buf.readInt();
      cfg.tickColor = buf.readInt();
      cfg.chime = buf.readBoolean();
      cfg.chimeSound = buf.readUtf(128);
      cfg.chimeMode = ClockConfig.ChimeMode.byName(buf.readUtf(16), cfg.chimeMode);
      cfg.tickStyle = ClockConfig.TickStyle.byName(buf.readUtf(16), cfg.tickStyle);
      cfg.redstone = buf.readBoolean();
      cfg.offsetMinutes = buf.readDouble();
      cfg.clamp();
      return cfg;
   }

   public static enum ChimeMode implements StringRepresentable {
      SINGLE("single"),
      HOURLY("hourly");

      private final String id;

      private ChimeMode(final String id) {
         this.id = id;
      }

      public String getSerializedName() {
         return this.id;
      }

      public String labelKey() {
         return "screen.mtrlte.clock.chime." + this.id;
      }

      static ClockConfig.ChimeMode byName(String name, ClockConfig.ChimeMode fallback) {
         for(ClockConfig.ChimeMode v : values()) {
            if (v.id.equalsIgnoreCase(name) || v.name().equalsIgnoreCase(name)) {
               return v;
            }
         }

         return fallback;
      }
   }

   public static enum GlowMode implements StringRepresentable {
      OFF("off"),
      DAY("day"),
      NIGHT("night"),
      ALWAYS("always");

      private final String id;

      private GlowMode(final String id) {
         this.id = id;
      }

      public String getSerializedName() {
         return this.id;
      }

      public String labelKey() {
         return "screen.mtrlte.clock.glow." + this.id;
      }

      static ClockConfig.GlowMode byName(String name, ClockConfig.GlowMode fallback) {
         for(ClockConfig.GlowMode v : values()) {
            if (v.id.equalsIgnoreCase(name) || v.name().equalsIgnoreCase(name)) {
               return v;
            }
         }

         return fallback;
      }
   }

   public static enum TickStyle implements StringRepresentable {
      NONE("none"),
      BAR("bar"),
      DOT("dot"),
      SQUARE("square"),
      TRIANGLE("triangle"),
      ARABIC("arabic"),
      ROMAN("roman");

      private final String id;

      private TickStyle(final String id) {
         this.id = id;
      }

      public String getSerializedName() {
         return this.id;
      }

      public String labelKey() {
         return "screen.mtrlte.clock.tick." + this.id;
      }

      public boolean isNumeral() {
         return this == ARABIC || this == ROMAN;
      }

      static ClockConfig.TickStyle byName(String name, ClockConfig.TickStyle fallback) {
         for(ClockConfig.TickStyle v : values()) {
            if (v.id.equalsIgnoreCase(name) || v.name().equalsIgnoreCase(name)) {
               return v;
            }
         }

         return fallback;
      }
   }

   public static enum TimeSource implements StringRepresentable {
      MC("mc"),
      REAL("real");

      private final String id;

      private TimeSource(final String id) {
         this.id = id;
      }

      public String getSerializedName() {
         return this.id;
      }

      public String labelKey() {
         return "screen.mtrlte.clock.source." + this.id;
      }

      static ClockConfig.TimeSource byName(String name, ClockConfig.TimeSource fallback) {
         for(ClockConfig.TimeSource v : values()) {
            if (v.id.equalsIgnoreCase(name) || v.name().equalsIgnoreCase(name)) {
               return v;
            }
         }

         return fallback;
      }
   }
}
