package com.github.thecrazy.noborders;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.lwjgl.opengl.Display;

import java.awt.Rectangle;
import java.lang.reflect.Field;

/**
 * Keeps a screen-sized OpenGL window in the desktop compositor on Windows.
 * WS_POPUP can trigger the driver's automatic exclusive-fullscreen promotion.
 * See https://github.com/libsdl-org/SDL/issues/12791.
 */
public final class WindowsBorderless {
    private static final int GWL_STYLE = -16;
    private static final int GWL_EXSTYLE = -20;
    private static final int WS_POPUP = 0x80000000;
    private static final int WS_CAPTION = 0x00C00000;
    private static final int WS_THICKFRAME = 0x00040000;
    private static final int WS_SYSMENU = 0x00080000;
    private static final int WS_MINIMIZEBOX = 0x00020000;
    private static final int WS_MAXIMIZEBOX = 0x00010000;
    private static final int WS_CLIPSIBLINGS = 0x04000000;
    private static final int WS_CLIPCHILDREN = 0x02000000;
    private static final int WS_EX_APPWINDOW = 0x00040000;
    private static final int WS_EX_TOPMOST = 0x00000008;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final int SWP_FRAMECHANGED = 0x0020;
    private static final int SWP_NOOWNERZORDER = 0x0200;

    private WindowsBorderless() { }

    public static void apply(Rectangle bounds) throws ReflectiveOperationException {
        // Resolve the actual LWJGL window, never another Java window or the foreground app.
        Field implementationField = Display.class.getDeclaredField("display_impl");
        implementationField.setAccessible(true);
        Object implementation = implementationField.get(null);
        Field handleField = implementation.getClass().getDeclaredField("hwnd");
        handleField.setAccessible(true);
        long handle = handleField.getLong(implementation);
        if (handle == 0) throw new IllegalStateException("LWJGL window has no native handle");
        HWND window = new HWND(new Pointer(handle));
        User32 user32 = User32.INSTANCE;

        int style = user32.GetWindowLong(window, GWL_STYLE);
        int windowedStyle = (style & ~(WS_POPUP | WS_CAPTION | WS_THICKFRAME | WS_SYSMENU
                | WS_MINIMIZEBOX | WS_MAXIMIZEBOX)) | WS_CLIPSIBLINGS | WS_CLIPCHILDREN;
        Native.setLastError(0);
        if (user32.SetWindowLong(window, GWL_STYLE, windowedStyle) == 0 && Native.getLastError() != 0) {
            throw new IllegalStateException("Could not set borderless window style: " + Native.getLastError());
        }
        int extended = user32.GetWindowLong(window, GWL_EXSTYLE);
        Native.setLastError(0);
        if (user32.SetWindowLong(window, GWL_EXSTYLE,
                (extended | WS_EX_APPWINDOW) & ~(WS_EX_TOPMOST | WS_EX_TOOLWINDOW)) == 0
                && Native.getLastError() != 0) {
            throw new IllegalStateException("Could not set borderless extended style: " + Native.getLastError());
        }
        if (!user32.SetWindowPos(window, null, bounds.x, bounds.y, bounds.width, bounds.height,
                SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED | SWP_NOOWNERZORDER)) {
            throw new IllegalStateException("Could not position borderless window: " + Native.getLastError());
        }
    }
}
