# Steam Deck Keyboard Mod — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a NeoForge 1.21.1 mod that provides a touch-screen virtual keyboard for Steam Deck, with manual toggle (default K key) and auto-popup on chat/JEI-EMI search fields.

**Architecture:** A client-only NeoForge mod using Mixin to intercept EditBox focus events and inject keyboard input. A KeyboardScreen overlay renders on top of the current screen with a QWERTY layout rendered as AbstractWidgets, supporting Shift toggle and drag-to-move, with position persisted in config.

**Tech Stack:** NeoForge 1.21.1, Java 21, ModDevGradle 2.x, Mixin 0.8.5, Minecraft's GuiGraphicsExtractor rendering

## Global Constraints

- Minecraft version: 1.21.1
- Mod loader: NeoForge (neo_version=21.1.234)
- Java: 21 (JavaLanguageVersion.of(21))
- Mod ID: `steamdeckkeyboard` (lowercase, matches [a-z][a-z0-9_]{1,63})
- Package: `com.steamdeck.keyboard`
- Client-only mod (@Mod(dist = Dist.CLIENT))
- Target: Steam Deck touchscreen only, no controller/gamepad support
- No animation or sound effects (pure touch click)
- All user-facing strings translatable via en_us.json
- Mixin config: `steamdeckkeyboard.mixins.json`

---

## File Structure

```
src/main/
├── java/com/steamdeck/keyboard/
│   ├── SteamDeckKeyboard.java            # @Mod entry point
│   ├── SteamDeckKeyboardClient.java      # Client-only @Mod, config + key mapping + events
│   ├── KeyboardConfig.java               # NeoForge ModConfigSpec
│   ├── KeyboardScreen.java               # Overlay Screen hosting the keyboard widget
│   ├── KeyboardWidget.java               # Container widget: layout + shift + touch dispatch
│   ├── KeyWidget.java                    # Single key: render + click handling
│   ├── KeyboardLayout.java               # QWERTY layout definition
│   ├── InputTarget.java                  # Interface for EditBox input operations
│   └── mixin/
│       ├── ChatScreenMixin.java          # ChatScreen: InputTarget impl + input box reposition
│       ├── EditBoxMixin.java             # EditBox: focus tracking for auto-open
│       └── ScreenMixin.java              # Screen: intercept K key toggle
├── resources/
│   ├── assets/steamdeckkeyboard/
│   │   └── lang/en_us.json               # English translations
│   └── steamdeckkeyboard.mixins.json     # Mixin configuration
├── templates/
│   └── META-INF/
│       └── neoforge.mods.toml            # Mod metadata template
```

---

### Task 1: Project Scaffolding

**Files:**
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `settings.gradle`
- Create: `src/main/templates/META-INF/neoforge.mods.toml`
- Create: `src/main/java/com/steamdeck/keyboard/SteamDeckKeyboard.java`
- Create: `src/main/java/com/steamdeck/keyboard/SteamDeckKeyboardClient.java`

**Interfaces:**
- Produces: `SteamDeckKeyboard.MODID = "steamdeckkeyboard"`, `SteamDeckKeyboard.LOGGER` (org.slf4j.Logger)

- [ ] **Step 1: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

parchment_minecraft_version=1.21.1
parchment_mappings_version=2024.11.17

minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.234
loader_version_range=[1,)

mod_id=steamdeckkeyboard
mod_name=Steam Deck Keyboard
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.steamdeck.keyboard
```

- [ ] **Step 2: Create `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}
```

- [ ] **Step 3: Create `build.gradle`**

```groovy
plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.141'
}

tasks.named('wrapper', Wrapper).configure {
    distributionType = Wrapper.DistributionType.BIN
}

version = mod_version
group = mod_group_id

sourceSets.main.resources {
    srcDir('src/generated/resources')
    exclude("**/*.bbmodel")
    exclude("src/generated/**/.cache")
}

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }
        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}

dependencies {
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}

var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            loader_version_range   : loader_version_range,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from "src/main/templates"
    into "build/generated/sources/modMetadata"
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

- [ ] **Step 4: Create source directories**

```bash
mkdir -p src/main/java/com/steamdeck/keyboard/mixin
mkdir -p src/main/templates/META-INF
mkdir -p src/main/resources/assets/steamdeckkeyboard/lang
```

- [ ] **Step 5: Write `src/main/templates/META-INF/neoforge.mods.toml`**

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
description='''
A touch-screen virtual keyboard for Steam Deck.
Press the configured key (default K) to toggle the keyboard overlay.
Automatically opens when chat or JEI/EMI search fields gain focus.
'''

[[mixins]]
config="${mod_id}.mixins.json"

[[dependencies.${mod_id}]]
modId="neoforge"
type="required"
versionRange="[${neo_version},)"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
type="required"
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

- [ ] **Step 6: Create `src/main/java/com/steamdeck/keyboard/SteamDeckKeyboard.java`**

```java
package com.steamdeck.keyboard;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SteamDeckKeyboard.MODID)
public class SteamDeckKeyboard {
    public static final String MODID = "steamdeckkeyboard";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SteamDeckKeyboard() {
        LOGGER.info("Steam Deck Keyboard mod loaded");
    }
}
```

- [ ] **Step 7: Create `src/main/java/com/steamdeck/keyboard/SteamDeckKeyboardClient.java`**

```java
package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean wasToggleKeyDown = false;

    public SteamDeckKeyboardClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, KeyboardConfig.SPEC);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKeyMapping = new KeyMapping(
            "key.steamdeckkeyboard.toggle",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            KeyboardConfig.TOGGLE_KEY_CODE.get(),
            "category.steamdeckkeyboard"
        );
        event.register(toggleKeyMapping);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean down = toggleKeyMapping != null && toggleKeyMapping.isDown();
        if (down && !wasToggleKeyDown) {
            if (KeyboardScreen.isOpen(mc)) {
                mc.setScreen(((KeyboardScreen) mc.screen).getBackgroundScreen());
            }
        }
        wasToggleKeyDown = down;
    }
}
```

- [ ] **Step 8: Initialize Gradle wrapper and verify build**

```bash
gradle wrapper --gradle-version 8.8
./gradlew build
```

Expected: BUILD SUCCESSFUL (will fail on missing classes referenced in later tasks, but scaffolding should resolve).

- [ ] **Step 9: Commit**

```bash
git add build.gradle gradle.properties settings.gradle gradle/wrapper/ gradlew gradlew.bat src/
git commit -m "feat: scaffold NeoForge 1.21.1 mod project structure"
```

---

### Task 2: KeyboardLayout + InputTarget + KeyboardConfig

**Files:**
- Create: `src/main/java/com/steamdeck/keyboard/KeyboardLayout.java`
- Create: `src/main/java/com/steamdeck/keyboard/InputTarget.java`
- Create: `src/main/java/com/steamdeck/keyboard/KeyboardConfig.java`

**Interfaces:**
- Produces: `KeyboardLayout.KeyType` enum (CHAR, BACKSPACE, ENTER, SHIFT, SPACE, CLOSE)
- Produces: `KeyboardLayout.KeyDef` record (String name, char normal, char shifted, KeyType keyType, float width)
- Produces: `KeyboardLayout.ROWS` (List<List<KeyDef>>)
- Produces: `KeyboardLayout.LAYOUT_WIDTH = 12f`
- Produces: `InputTarget.SpecialKey` enum (BACKSPACE, ENTER)
- Produces: `InputTarget` interface: `acceptChar(char)`, `acceptSpecial(SpecialKey)`, `supportsCharInput()`
- Produces: `KeyboardConfig.SPEC` (ModConfigSpec) with all config fields

- [ ] **Step 1: Create `KeyboardLayout.java`**

```java
package com.steamdeck.keyboard;

import java.util.List;

public final class KeyboardLayout {
    public enum KeyType { CHAR, BACKSPACE, ENTER, SHIFT, SPACE, CLOSE }

    public record KeyDef(String name, char normal, char shifted, KeyType keyType, float width) {}

    public static final List<List<KeyDef>> ROWS = List.of(
        List.of(
            new KeyDef("q", 'q', 'Q', KeyType.CHAR, 1f),
            new KeyDef("w", 'w', 'W', KeyType.CHAR, 1f),
            new KeyDef("e", 'e', 'E', KeyType.CHAR, 1f),
            new KeyDef("r", 'r', 'R', KeyType.CHAR, 1f),
            new KeyDef("t", 't', 'T', KeyType.CHAR, 1f),
            new KeyDef("y", 'y', 'Y', KeyType.CHAR, 1f),
            new KeyDef("u", 'u', 'U', KeyType.CHAR, 1f),
            new KeyDef("i", 'i', 'I', KeyType.CHAR, 1f),
            new KeyDef("o", 'o', 'O', KeyType.CHAR, 1f),
            new KeyDef("p", 'p', 'P', KeyType.CHAR, 1f),
            new KeyDef("backspace", '\b', '\b', KeyType.BACKSPACE, 2f)
        ),
        List.of(
            new KeyDef("a", 'a', 'A', KeyType.CHAR, 1f),
            new KeyDef("s", 's', 'S', KeyType.CHAR, 1f),
            new KeyDef("d", 'd', 'D', KeyType.CHAR, 1f),
            new KeyDef("f", 'f', 'F', KeyType.CHAR, 1f),
            new KeyDef("g", 'g', 'G', KeyType.CHAR, 1f),
            new KeyDef("h", 'h', 'H', KeyType.CHAR, 1f),
            new KeyDef("j", 'j', 'J', KeyType.CHAR, 1f),
            new KeyDef("k", 'k', 'K', KeyType.CHAR, 1f),
            new KeyDef("l", 'l', 'L', KeyType.CHAR, 1f),
            new KeyDef("slash", '/', '?', KeyType.CHAR, 1f),
            new KeyDef("enter", '\n', '\n', KeyType.ENTER, 2f)
        ),
        List.of(
            new KeyDef("shift", '\0', '\0', KeyType.SHIFT, 2f),
            new KeyDef("z", 'z', 'Z', KeyType.CHAR, 1f),
            new KeyDef("x", 'x', 'X', KeyType.CHAR, 1f),
            new KeyDef("c", 'c', 'C', KeyType.CHAR, 1f),
            new KeyDef("v", 'v', 'V', KeyType.CHAR, 1f),
            new KeyDef("b", 'b', 'B', KeyType.CHAR, 1f),
            new KeyDef("n", 'n', 'N', KeyType.CHAR, 1f),
            new KeyDef("m", 'm', 'M', KeyType.CHAR, 1f),
            new KeyDef("comma", ',', ';', KeyType.CHAR, 1f),
            new KeyDef("period", '.', '\'', KeyType.CHAR, 1f)
        ),
        List.of(
            new KeyDef("space", ' ', ' ', KeyType.SPACE, 4f),
            new KeyDef("at", '@', '(', KeyType.CHAR, 1f),
            new KeyDef("hash", '#', ')', KeyType.CHAR, 1f),
            new KeyDef("dollar", '$', '-', KeyType.CHAR, 1f),
            new KeyDef("percent", '%', '+', KeyType.CHAR, 1f),
            new KeyDef("exclaim", '!', '=', KeyType.CHAR, 1f),
            new KeyDef("colon", ':', '[', KeyType.CHAR, 1f),
            new KeyDef("underscore", '_', '{', KeyType.CHAR, 1f),
            new KeyDef("ampersand", '&', '}', KeyType.CHAR, 1f),
            new KeyDef("close", '\0', '\0', KeyType.CLOSE, 1f)
        )
    );

    public static final float LAYOUT_WIDTH = 12f;

    private KeyboardLayout() {}
}
```

- [ ] **Step 2: Create `InputTarget.java`**

```java
package com.steamdeck.keyboard;

public interface InputTarget {
    enum SpecialKey { BACKSPACE, ENTER }

    void acceptChar(char ch);
    void acceptSpecial(SpecialKey key);

    default boolean supportsCharInput() { return true; }
}
```

- [ ] **Step 3: Create `KeyboardConfig.java`**

```java
package com.steamdeck.keyboard;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

public class KeyboardConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TOGGLE_KEY_CODE = BUILDER
            .comment("Default key code for toggle keyboard (GLFW_KEY_K = 75)")
            .defineInRange("toggleKeyCode", GLFW.GLFW_KEY_K, 0, 348);

    public static final ModConfigSpec.BooleanValue AUTO_OPEN_CHAT = BUILDER
            .comment("Auto-open keyboard when chat field gains focus")
            .define("autoOpenChat", true);

    public static final ModConfigSpec.BooleanValue AUTO_OPEN_SEARCH = BUILDER
            .comment("Auto-open keyboard when JEI/EMI search field gains focus")
            .define("autoOpenSearch", true);

    public static final ModConfigSpec.BooleanValue AUTO_OPEN_OTHERS = BUILDER
            .comment("Auto-open keyboard when other EditBox gains focus")
            .define("autoOpenOthers", false);

    public static final ModConfigSpec.IntValue KEYBOARD_X = BUILDER
            .comment("Saved keyboard X position (-1 = center)")
            .defineInRange("keyboardX", -1, -1, 10000);

    public static final ModConfigSpec.IntValue KEYBOARD_Y = BUILDER
            .comment("Saved keyboard Y position (-1 = auto near bottom)")
            .defineInRange("keyboardY", -1, -1, 10000);

    public static final ModConfigSpec.IntValue KEYBOARD_WIDTH = BUILDER
            .comment("Saved keyboard width (-1 = 90% of screen)")
            .defineInRange("keyboardWidth", -1, -1, 10000);

    public static final ModConfigSpec.IntValue KEYBOARD_HEIGHT = BUILDER
            .comment("Saved keyboard height (-1 = 30% of screen)")
            .defineInRange("keyboardHeight", -1, -1, 10000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/steamdeck/keyboard/KeyboardLayout.java src/main/java/com/steamdeck/keyboard/InputTarget.java src/main/java/com/steamdeck/keyboard/KeyboardConfig.java
git commit -m "feat: add layout, input target, and config definitions"
```

---

### Task 3: KeyWidget + KeyboardWidget

**Files:**
- Create: `src/main/java/com/steamdeck/keyboard/KeyWidget.java`
- Create: `src/main/java/com/steamdeck/keyboard/KeyboardWidget.java`

**Interfaces:**
- Consumes: `KeyboardLayout.KeyDef`, `KeyboardLayout.KeyType`, `KeyboardLayout.ROWS`, `KeyboardLayout.LAYOUT_WIDTH`, `InputTarget`
- Produces: `KeyWidget` extends `AbstractWidget`
- Produces: `KeyboardWidget` extends `AbstractWidget` implements `ContainerEventHandler`

- [ ] **Step 1: Create `KeyWidget.java`**

```java
package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class KeyWidget extends AbstractWidget {
    private static final int COLOR_BG = 0xC0444444;
    private static final int COLOR_BG_HOVER = 0xC0666666;
    private static final int COLOR_BG_PRESSED = 0xC0888888;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final KeyboardLayout.KeyDef keyDef;
    private final Runnable onPress;
    private boolean pressed;

    public KeyWidget(int x, int y, int width, int height, KeyboardLayout.KeyDef keyDef, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.keyDef = keyDef;
        this.onPress = onPress;
    }

    public KeyboardLayout.KeyType getKeyType() { return keyDef.keyType(); }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float a) {
        int bg = pressed ? COLOR_BG_PRESSED : (isHovered() ? COLOR_BG_HOVER : COLOR_BG);
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, bg);
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + 2, 0x40FFFFFF);
        g.fill(getX() + 1, getY() + getHeight() - 2, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x40000000);
    }

    public void renderLabel(GuiGraphicsExtractor g, boolean shifted) {
        String label = switch (keyDef.keyType()) {
            case CHAR -> String.valueOf(shifted ? keyDef.shifted() : keyDef.normal());
            case SHIFT -> shifted ? "\u21EA" : "\u21E7";
            case SPACE -> "";
            case BACKSPACE -> "\u2190";
            case ENTER -> "\u21B5";
            case CLOSE -> "\u00D7";
        };
        int tw = Minecraft.getInstance().font.width(label);
        int tx = getX() + (getWidth() - tw) / 2;
        int ty = getY() + (getHeight() - 8) / 2 + (pressed ? 1 : 0);
        g.text(Minecraft.getInstance().font, label, tx, ty, COLOR_TEXT);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent ev, boolean dbl) {
        if (isMouseOver(ev.x(), ev.y()) && isActive()) { pressed = true; onPress.run(); return true; }
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent ev) { pressed = false; return super.mouseReleased(ev); }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput o) {}
}
```

- [ ] **Step 2: Create `KeyboardWidget.java`**

```java
package com.steamdeck.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class KeyboardWidget extends AbstractWidget implements ContainerEventHandler {
    private final List<KeyWidget> keys;
    private final InputTarget inputTarget;
    private final Runnable onClose;
    private boolean shifted;
    private boolean shiftLocked;
    private @Nullable KeyWidget focusedKey;
    private boolean dragging;

    public KeyboardWidget(int x, int y, int w, int h, InputTarget inputTarget, Runnable onClose) {
        super(x, y, w, h, Component.literal("Keyboard"));
        this.inputTarget = inputTarget;
        this.onClose = onClose;
        this.keys = new ArrayList<>();
        float uw = (float) w / KeyboardLayout.LAYOUT_WIDTH;
        float kh = (float) h / KeyboardLayout.ROWS.size();
        float yPos = y;
        for (List<KeyboardLayout.KeyDef> row : KeyboardLayout.ROWS) {
            float xPos = x;
            for (KeyboardLayout.KeyDef def : row) {
                float kw = def.width() * uw;
                keys.add(new KeyWidget((int) xPos, (int) yPos, (int) kw, (int) kh, def,
                    () -> onKeyPress(def)));
                xPos += kw;
            }
            yPos += kh;
        }
    }

    private void onKeyPress(KeyboardLayout.KeyDef def) {
        switch (def.keyType()) {
            case CHAR -> {
                inputTarget.acceptChar(shifted ? def.shifted() : def.normal());
                if (shifted && !shiftLocked) setShifted(false);
            }
            case SPACE -> {
                inputTarget.acceptChar(' ');
                if (shifted && !shiftLocked) setShifted(false);
            }
            case BACKSPACE -> inputTarget.acceptSpecial(InputTarget.SpecialKey.BACKSPACE);
            case ENTER -> inputTarget.acceptSpecial(InputTarget.SpecialKey.ENTER);
            case SHIFT -> {
                if (!shifted) setShifted(true);
                else if (!shiftLocked) setShiftLocked(true);
                else { setShifted(false); setShiftLocked(false); }
            }
            case CLOSE -> onClose.run();
        }
    }

    public void setShifted(boolean s) { shifted = s; }
    private void setShiftLocked(boolean l) { shiftLocked = l; }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float a) {
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0141414);
        g.outline(getX(), getY(), getWidth(), getHeight(), 0xFF666666);
        for (KeyWidget k : keys) k.extractRenderState(g, mx, my, a);
        for (KeyWidget k : keys) k.renderLabel(g, shifted);
    }

    @Override public @NonNull List<KeyWidget> children() { return Collections.unmodifiableList(keys); }
    @Override public boolean isDragging() { return dragging; }
    @Override public void setDragging(boolean d) { dragging = d; }
    @Override public @Nullable KeyWidget getFocused() { return focusedKey; }
    @Override public void setFocused(@Nullable GuiEventListener l) {
        if (focusedKey != null) focusedKey.setFocused(false);
        if (l instanceof KeyWidget kw) { focusedKey = kw; kw.setFocused(true); }
        else focusedKey = null;
    }
    @Override public @Nullable net.minecraft.client.gui.ComponentPath nextFocusPath(FocusNavigationEvent e) { return ContainerEventHandler.super.nextFocusPath(e); }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean d) { return ContainerEventHandler.super.mouseClicked(e, d); }
    @Override public boolean mouseReleased(MouseButtonEvent e) { return ContainerEventHandler.super.mouseReleased(e); }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return ContainerEventHandler.super.mouseDragged(e, dx, dy); }
    @Override protected void updateWidgetNarration(NarrationElementOutput o) {}
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/steamdeck/keyboard/KeyWidget.java src/main/java/com/steamdeck/keyboard/KeyboardWidget.java
git commit -m "feat: add KeyWidget and KeyboardWidget with rendering and touch input"
```

---

### Task 4: KeyboardScreen — Overlay with Drag

**Files:**
- Create: `src/main/java/com/steamdeck/keyboard/KeyboardScreen.java`

**Interfaces:**
- Consumes: `KeyboardWidget`, `KeyboardConfig`, `InputTarget`
- Produces: `KeyboardScreen extends Screen`
- Produces: `KeyboardScreen.open(Screen, InputTarget)` static method
- Produces: `KeyboardScreen.isOpen(Minecraft)` static method
- Produces: `KeyboardScreen.getBackgroundScreen()` method

- [ ] **Step 1: Create `KeyboardScreen.java`**

```java
package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class KeyboardScreen extends Screen {
    private final Screen backgroundScreen;
    private final InputTarget inputTarget;
    private KeyboardWidget keyboardWidget;
    private int dragStartX, dragStartY, dragOffsetX, dragOffsetY;
    private boolean dragging;

    public KeyboardScreen(Screen backgroundScreen, InputTarget inputTarget) {
        super(Component.literal("Keyboard Overlay"));
        this.backgroundScreen = backgroundScreen;
        this.inputTarget = inputTarget;
    }

    @Override
    protected void init() {
        int kbdW = KeyboardConfig.KEYBOARD_WIDTH.get();
        int kbdH = KeyboardConfig.KEYBOARD_HEIGHT.get();
        if (kbdW <= 0) kbdW = (int) (width * 0.9);
        if (kbdH <= 0) kbdH = (int) (height * 0.3);
        int kbdX = KeyboardConfig.KEYBOARD_X.get();
        int kbdY = KeyboardConfig.KEYBOARD_Y.get();
        if (kbdX < 0) kbdX = (width - kbdW) / 2;
        if (kbdY < 0) kbdY = height - kbdH - 10;

        keyboardWidget = new KeyboardWidget(kbdX, kbdY, kbdW, kbdH, inputTarget, this::onClose);
        addRenderableWidget(keyboardWidget);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float a) {
        backgroundScreen.extractRenderState(g, mx, my, a);
        g.nextStratum();
        super.extractRenderState(g, mx, my, a);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor g, int mx, int my, float a) {
        backgroundScreen.extractBackground(g, mx, my, a);
    }

    @Override
    public void tick() { backgroundScreen.tick(); super.tick(); }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent ev, boolean dbl) {
        if (super.mouseClicked(ev, dbl)) return true;
        if (keyboardWidget != null && keyboardWidget.isMouseOver(ev.x(), ev.y())) {
            dragging = true;
            dragStartX = (int) ev.x(); dragStartY = (int) ev.y();
            dragOffsetX = keyboardWidget.getX(); dragOffsetY = keyboardWidget.getY();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent ev, double dx, double dy) {
        if (dragging && keyboardWidget != null) {
            int nx = dragOffsetX + (int) ev.x() - dragStartX;
            int ny = dragOffsetY + (int) ev.y() - dragStartY;
            keyboardWidget.setX(Math.max(0, Math.min(nx, width - keyboardWidget.getWidth())));
            keyboardWidget.setY(Math.max(0, Math.min(ny, height - keyboardWidget.getHeight())));
            return true;
        }
        return super.mouseDragged(ev, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent ev) {
        dragging = false;
        if (keyboardWidget != null) {
            KeyboardConfig.KEYBOARD_X.set(keyboardWidget.getX());
            KeyboardConfig.KEYBOARD_Y.set(keyboardWidget.getY());
            KeyboardConfig.KEYBOARD_WIDTH.set(keyboardWidget.getWidth());
            KeyboardConfig.KEYBOARD_HEIGHT.set(keyboardWidget.getHeight());
            KeyboardConfig.SPEC.save();
        }
        return super.mouseReleased(ev);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(backgroundScreen);
    }

    public Screen getBackgroundScreen() { return backgroundScreen; }

    public static void open(Screen currentScreen, InputTarget target) {
        if (currentScreen instanceof KeyboardScreen) return;
        Minecraft.getInstance().setScreen(new KeyboardScreen(currentScreen, target));
    }

    public static boolean isOpen(Minecraft mc) { return mc.screen instanceof KeyboardScreen; }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/steamdeck/keyboard/KeyboardScreen.java
git commit -m "feat: add KeyboardScreen overlay with drag-to-move and position persistence"
```

---

### Task 5: Mixin Configuration + ChatScreenMixin + EditBoxMixin

**Files:**
- Create: `src/main/resources/steamdeckkeyboard.mixins.json`
- Create: `src/main/java/com/steamdeck/keyboard/mixin/ChatScreenMixin.java`
- Create: `src/main/java/com/steamdeck/keyboard/mixin/EditBoxMixin.java`

**Interfaces:**
- Consumes: `InputTarget`, `KeyboardScreen`, `KeyboardConfig`
- Produces: `ChatScreenMixin` implements `InputTarget` on `ChatScreen`, auto-opens keyboard, repositions input box
- Produces: `EditBoxMixin` intercepts `setFocused(true)` to trigger auto-open

- [ ] **Step 1: Create `src/main/resources/steamdeckkeyboard.mixins.json`**

```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "com.steamdeck.keyboard.mixin",
    "compatibilityLevel": "JAVA_21",
    "refmap": "steamdeckkeyboard.refmap.json",
    "client": [
        "ChatScreenMixin",
        "EditBoxMixin"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

- [ ] **Step 2: Create `src/main/java/com/steamdeck/keyboard/mixin/ChatScreenMixin.java`**

```java
package com.steamdeck.keyboard.mixin;

import com.steamdeck.keyboard.InputTarget;
import com.steamdeck.keyboard.KeyboardConfig;
import com.steamdeck.keyboard.KeyboardScreen;
import com.steamdeck.keyboard.SteamDeckKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements InputTarget {
    @Shadow protected EditBox input;
    @Unique private float keyboardShiftAmount = 0f;

    protected ChatScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (KeyboardConfig.AUTO_OPEN_CHAT.get() && !KeyboardScreen.isOpen(minecraft)) {
            KeyboardScreen.open(this, this);
            keyboardShiftAmount = 0.25f;
            int keyboardHeight = (int) (height * keyboardShiftAmount);
            input.setY(height - keyboardHeight - 12 - 2);
        }
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 1)
    private int modifyEditBoxY(int y) {
        if (keyboardShiftAmount > 0) {
            return height - (int)(height * keyboardShiftAmount) - 12;
        }
        return y;
    }

    @Override
    public void acceptChar(char ch) {
        input.charTyped(new CharacterEvent(ch));
    }

    @Override
    public void acceptSpecial(SpecialKey key) {
        switch (key) {
            case BACKSPACE -> {
                String value = input.getValue();
                if (!value.isEmpty()) {
                    input.setValue(value.substring(0, value.length() - 1));
                }
            }
            case ENTER -> {
                if (minecraft != null) {
                    minecraft.setScreen(null);
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create `src/main/java/com/steamdeck/keyboard/mixin/EditBoxMixin.java`**

```java
package com.steamdeck.keyboard.mixin;

import com.steamdeck.keyboard.KeyboardConfig;
import com.steamdeck.keyboard.KeyboardScreen;
import com.steamdeck.keyboard.SteamDeckKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public class EditBoxMixin {

    @Inject(method = "setFocused", at = @At("HEAD"))
    private void onSetFocused(boolean focused, CallbackInfo ci) {
        if (!focused) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || KeyboardScreen.isOpen(mc)) return;

        boolean isChat = mc.screen instanceof ChatScreen;
        boolean isSearch = isSearchScreen(mc.screen);

        // Check if this EditBox is the input field on the current screen
        // Auto-open if conditions match
        if (isChat && KeyboardConfig.AUTO_OPEN_CHAT.get()) {
            if (mc.screen instanceof ChatScreen cs) {
                KeyboardScreen.open(cs, (com.steamdeck.keyboard.InputTarget) cs);
            }
        } else if (isSearch && KeyboardConfig.AUTO_OPEN_SEARCH.get()) {
            KeyboardScreen.open(mc.screen, createGenericInputTarget((EditBox)(Object)this));
        } else if (KeyboardConfig.AUTO_OPEN_OTHERS.get()) {
            KeyboardScreen.open(mc.screen, createGenericInputTarget((EditBox)(Object)this));
        }
    }

    private static boolean isSearchScreen(Screen screen) {
        String className = screen.getClass().getName();
        return className.contains("jei") || className.contains("emi");
    }

    private static com.steamdeck.keyboard.InputTarget createGenericInputTarget(EditBox editBox) {
        return new com.steamdeck.keyboard.InputTarget() {
            @Override
            public void acceptChar(char ch) {
                editBox.charTyped(new net.minecraft.client.input.CharacterEvent(ch));
            }
            @Override
            public void acceptSpecial(SpecialKey key) {
                switch (key) {
                    case BACKSPACE -> {
                        String val = editBox.getValue();
                        if (!val.isEmpty()) editBox.setValue(val.substring(0, val.length() - 1));
                    }
                    case ENTER -> Minecraft.getInstance().setScreen(null);
                }
            }
        };
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/steamdeckkeyboard.mixins.json src/main/java/com/steamdeck/keyboard/mixin/
git commit -m "feat: add mixin config, ChatScreen + EditBox mixins for auto-open and input injection"
```

---

### Task 6: English Translations + Final Integration

**Files:**
- Create: `src/main/resources/assets/steamdeckkeyboard/lang/en_us.json`

**Interfaces:**
- Consumes: `SteamDeckKeyboard.MODID`

- [ ] **Step 1: Create `en_us.json`**

```json
{
  "key.steamdeckkeyboard.toggle": "Toggle Keyboard",
  "category.steamdeckkeyboard": "Steam Deck Keyboard"
}
```

- [ ] **Step 2: Verify full build**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/steamdeckkeyboard/lang/en_us.json
git commit -m "feat: add English translations and finalize integration"
```
