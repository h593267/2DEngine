package no.arnemunthekaas.engine.listeners;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Class handling the engines MouseListener, all methods are static and uses the get() method so that there
 * can only be one instance of this class. See https://www.glfw.org/docs/3.3/input_guide.html
 */
public class MouseListener {
    private static MouseListener instance;
    private double scrollX, scrollY;
    double xPos, yPos, lastY, lastX;
    private boolean mouseButtonPressed[] = new boolean[GLFW_MOUSE_BUTTON_LAST+1]; // <- how many mouse buttons program supports. See https://www.glfw.org/docs/3.3/group__buttons.html
    private boolean isDragging;

    private MouseListener() {
        this.scrollX = 0.0;
        this.scrollY = 0.0;
        this.xPos = 0.0;
        this.yPos = 0.0;
        this.lastX = 0.0;
        this.lastY = 0.0;
    }

    /**
     * Returns the MouseListener instance, if null creates new MouseListener
     * This is so that there is one and only one MouseListener at all times.
     *
     * @return MouseListener instance
     */
    public static MouseListener get() {
        if (MouseListener.instance == null)
            MouseListener.instance = new MouseListener();
        return MouseListener.instance;
    }

    /**
     * @param window window reference
     * @param xPos   new x-position for mouse
     * @param yPos   new y-position for mouse
     */
    public static void mousePosCallback(long window, double xPos, double yPos) {
        get().lastX = get().xPos;
        get().lastY = get().yPos;
        get().xPos = xPos;
        get().yPos = yPos;
        get().isDragging = get().mouseButtonPressed[0] || get().mouseButtonPressed[1] || get().mouseButtonPressed[2]; // <- Support for dragging mouse with buttons 1-3
    }

    /**
     * @param window window reference
     * @param button mouse button action is performed on
     * @param action what the mouse action is, e.g. press/release
     * @param mods   modifier keys, e.g. ctrl or shift
     */
    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        if (action == GLFW_PRESS) {
            if (button < get().mouseButtonPressed.length)
                get().mouseButtonPressed[button] = true;
        } else if (action == GLFW_RELEASE) {
            if (button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = false;
                get().isDragging = false;
            }
        }
    }

    /**
     * @param window  window reference
     * @param xOffset new xOffset
     * @param yOffset new yOffset
     */
    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
    }

    /**
     * Updates after every frame
     */
    public static void endFrame() {
        get().scrollX = 0;
        get().scrollY = 0;
        get().lastX = get().xPos;
        get().lastY = get().yPos;
    }

    // Static getters and setters for single MouseListener Instance

    /**
     * Get MouseListener x-position
     *
     * @return xPos
     */
    public static float getX() {
        return (float) get().xPos;
    }

    /**
     * Get MouseListener y-position
     *
     * @return yPos
     */
    public static float getY() {
        return (float) get().yPos;
    }

    /**
     * Get MouseListener change in x-position
     *
     * @return dX
     */
    public static float getDx() {
        return (float) (get().lastX - get().xPos);
    }

    /**
     * Get MouseListener change in y-position
     *
     * @return dY
     */
    public static float getDy() {
        return (float) (get().lastY - get().yPos);
    }

    /**
     * Get MouseListener scroll in x-axis
     *
     * @return scrollX
     */
    public static float getScrollX() {
        return (float) get().scrollX;
    }

    /**
     * Get MouseListener scroll in y-axis
     *
     * @return scrollY
     */
    public static float getScrollY() {
        return (float) get().scrollY;
    }

    /**
     * Get MouseListener dragging
     *
     * @return isDragging
     */
    public static boolean isDragging() {
        return get().isDragging;
    }

    /**
     * Checks if a bytton is pressed
     *
     * @param button Button to check if pressed
     * @return true if pressed
     */
    public static boolean mouseButtonDown(int button) {
        return get().mouseButtonPressed[button]; // Could check for IndexOutOfBoundsException, but won't for possible debugging scenarios
    }

}