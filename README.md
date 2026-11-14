# No Borders

Client-side Minecraft **1.8.9 / Forge 11.15.1.2318** mod with Mixin-based display modes.

In **Options > Video Settings**, the vanilla Fullscreen button becomes **Display Mode**.
Click it, or press the configured fullscreen key (F11 by default), to cycle:

**Windowed > Fullscreen > Borderless > Windowed**

Borderless is an undecorated window covering the monitor where the game was windowed.
It uses that monitor's full desktop bounds, including the taskbar area, without entering
exclusive fullscreen. Returning to windowed mode restores the previous window position
and client size for the current session. Exclusive fullscreen uses Minecraft's normal
display-mode selection.

On Windows, borderless also removes the native WS_POPUP style. This prevents the OpenGL
driver from treating a screen-sized popup as exclusive fullscreen when focus changes.
Saved borderless sessions start windowed before applying borderless, avoiding an initial
exclusive display-mode switch. Reference: https://github.com/libsdl-org/SDL/issues/12791.

The selected mode is saved immediately to 'config/noborders.properties' in the game
directory and restored on the next launch. Without that file, the mod respects
Minecraft's existing fullscreen preference. The file accepts 'mode=WINDOWED',
'mode=FULLSCREEN', or 'mode=BORDERLESS'. Minecraft's own fullscreen flag is enabled only
for exclusive fullscreen; borderless remains a native windowed display.

## Build

Use JDK 17 to run Gradle and install JDK 8 for the Minecraft toolchain.

Windows:

    .\gradlew.bat build

Linux/macOS:

    ./gradlew build

Install 'build/libs/noborders-1.0.0.jar' in the Forge instance's 'mods' directory.
Development client: './gradlew runClientCompat'. This task works around the template's
Loom 0.10 / Gradle 8 launch incompatibility and uses the 1.8.9 LaunchWrapper entry point.

## Manual verification

- Cycle all three modes using Video Settings and F11, both at the main menu and in a world.
- Resize and move the window, cycle through the modes, and check its restored size/position.
- In borderless mode, check that no title bar/borders are present, the desktop is covered,
  Alt-Tab works, mouse capture returns in-game, and menus and rendering fill the window.
- Move the window to another monitor before cycling and check borderless fills that monitor.
- Restart in each mode and check the saved mode is restored.
- Check VSync and different GUI scales while switching.

Mixed-DPI monitor setups and window-manager-specific behavior need testing on the
target desktop. The implementation uses LWJGL 2 native window recreation and AWT monitor
bounds; exclusive fullscreen retains Minecraft 1.8.9's monitor limitations.

Based on the Architectury Loom Forge 1.8.9 template.
