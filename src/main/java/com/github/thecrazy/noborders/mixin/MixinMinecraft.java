package com.github.thecrazy.noborders.mixin;

import com.github.thecrazy.noborders.WindowMode;
import com.github.thecrazy.noborders.WindowsBorderless;
import com.github.thecrazy.noborders.WindowModeAccess;
import com.github.thecrazy.noborders.WindowModeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements WindowModeAccess {
    @Shadow private boolean fullscreen;
    @Shadow private int tempDisplayWidth;
    @Shadow private int tempDisplayHeight;
    @Shadow public GameSettings gameSettings;
    @Shadow @Final public File mcDataDir;
    @Shadow public abstract void resize(int width, int height);
    @Shadow private void updateDisplayMode() throws LWJGLException { }

    @Unique private WindowMode noborders$mode;
    @Unique private boolean noborders$ready;
    @Unique private Rectangle noborders$window;
    @Unique private Rectangle noborders$monitor;

    @Override
    public WindowMode noborders$getMode() {
        return noborders$mode != null ? noborders$mode
                : (fullscreen ? WindowMode.FULLSCREEN : WindowMode.WINDOWED);
    }

    @Inject(method = "setInitialDisplayMode", at = @At("HEAD"))
    private void noborders$avoidExclusiveStartup(CallbackInfo ci) {
        // A saved borderless session must never start in exclusive fullscreen first.
        if (WindowModeConfig.load(mcDataDir, WindowMode.WINDOWED) == WindowMode.BORDERLESS) {
            fullscreen = false;
            gameSettings.fullScreen = false;
        }
    }

    @Inject(method = "startGame", at = @At("RETURN"))
    private void noborders$restoreMode(CallbackInfo ci) {
        noborders$mode = fullscreen ? WindowMode.FULLSCREEN : WindowMode.WINDOWED;
        noborders$window = new Rectangle(Display.getX(), Display.getY(),
                Math.max(1, tempDisplayWidth), Math.max(1, tempDisplayHeight));
        noborders$monitor = noborders$findMonitor();
        noborders$ready = true;
        WindowMode saved = WindowModeConfig.load(mcDataDir, noborders$mode);
        if (saved != noborders$mode) noborders$apply(saved);
    }

    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
    private void noborders$cycleMode(CallbackInfo ci) {
        // Leave vanilla's early startup toggle alone, before the renderer is ready.
        if (!noborders$ready) return;
        noborders$apply(noborders$mode.next());
        ci.cancel();
    }

    @Unique
    private void noborders$apply(WindowMode target) {
        WindowMode previous = noborders$mode;
        if (previous == WindowMode.WINDOWED) {
            noborders$window = new Rectangle(Display.getX(), Display.getY(),
                    Display.getWidth(), Display.getHeight());
            noborders$monitor = noborders$findMonitor();
            tempDisplayWidth = noborders$window.width;
            tempDisplayHeight = noborders$window.height;
        }
        try {
            noborders$setDisplay(target);
            noborders$mode = target;
        } catch (Exception exception) {
            LogManager.getLogger("NoBorders").error("Could not switch to " + target, exception);
            try {
                noborders$setDisplay(previous);
            } catch (Exception recovery) {
                LogManager.getLogger("NoBorders").error("Could not restore previous display mode", recovery);
            }
        }
        fullscreen = noborders$mode == WindowMode.FULLSCREEN;
        gameSettings.fullScreen = fullscreen;
        resize(Math.max(1, Display.getWidth()), Math.max(1, Display.getHeight()));
        Display.setVSyncEnabled(gameSettings.enableVsync);
        WindowModeConfig.save(mcDataDir, noborders$mode);
        gameSettings.saveOptions();
    }

    @Unique
    private void noborders$setDisplay(WindowMode mode) throws LWJGLException, ReflectiveOperationException {
        // LWJGL 2 reads this property when it recreates the native window.
        System.setProperty("org.lwjgl.opengl.Window.undecorated",
                Boolean.toString(mode == WindowMode.BORDERLESS));
        if (mode == WindowMode.FULLSCREEN) {
            updateDisplayMode();
            Display.setFullscreen(true);
        } else {
            Display.setFullscreen(false);
            Rectangle bounds = mode == WindowMode.BORDERLESS ? noborders$monitor : noborders$window;
            // setDisplayMode recreates the native window, applying its decorations.
            Display.setDisplayMode(new DisplayMode(bounds.width, bounds.height));
            Display.setLocation(bounds.x, bounds.y);
        }
        // Refresh resize styles after recreation (required by older LWJGL 2 builds).
        Display.setResizable(false);
        Display.setResizable(mode == WindowMode.WINDOWED);
        if (mode == WindowMode.BORDERLESS && LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS) {
            WindowsBorderless.apply(noborders$monitor);
        }
    }

    @Unique
    private Rectangle noborders$findMonitor() {
        Rectangle window = new Rectangle(Display.getX(), Display.getY(), Display.getWidth(), Display.getHeight());
        Rectangle best = new Rectangle(0, 0, Display.getDesktopDisplayMode().getWidth(),
                Display.getDesktopDisplayMode().getHeight());
        try {
            long largest = -1;
            for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                Rectangle overlap = bounds.intersection(window);
                long area = overlap.isEmpty() ? 0 : (long) overlap.width * overlap.height;
                if (area > largest) {
                    largest = area;
                    best = bounds;
                }
            }
        } catch (RuntimeException exception) {
            LogManager.getLogger("NoBorders").warn("Using primary monitor bounds", exception);
        }
        return best;
    }
}




