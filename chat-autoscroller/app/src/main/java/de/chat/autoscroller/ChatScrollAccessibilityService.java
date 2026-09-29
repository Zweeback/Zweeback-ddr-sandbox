package de.chat.autoscroller;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatScrollAccessibilityService extends AccessibilityService {
    private static ChatScrollAccessibilityService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlay;
    private WindowManager.LayoutParams overlayParams;

    private int direction = 0;
    private int speed = 3;
    private boolean gestureBusy = false;
    private TextView stateLabel;
    private TextView speedLabel;

    private final Runnable scrollLoop = new Runnable() {
        @Override
        public void run() {
            if (direction == 0) return;
            scrollOnce(direction);
            handler.postDelayed(this, intervalForSpeed());
        }
    };

    public static ChatScrollAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showOverlay();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        stopAutoScroll();
    }

    @Override
    public void onDestroy() {
        stopAutoScroll();
        removeOverlay();
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public void showOverlay() {
        if (windowManager == null || overlay != null) return;

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(8));
        panel.setBackground(roundedBackground(Color.argb(235, 32, 32, 32), dp(14)));

        TextView handle = new TextView(this);
        handle.setText("↕  CHAT SCROLL");
        handle.setTextColor(Color.WHITE);
        handle.setTextSize(12f);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(dp(8), dp(5), dp(8), dp(7));
        panel.addView(handle, new LinearLayout.LayoutParams(dp(124), dp(38)));

        Button up = makeButton("↑");
        up.setContentDescription("Automatisch nach oben scrollen");
        up.setOnClickListener(v -> startAutoScroll(-1));
        panel.addView(up, new LinearLayout.LayoutParams(dp(124), dp(54)));

        Button stop = makeButton("■ STOP");
        stop.setContentDescription("Automatisches Scrollen stoppen");
        stop.setOnClickListener(v -> stopAutoScroll());
        panel.addView(stop, new LinearLayout.LayoutParams(dp(124), dp(48)));

        Button down = makeButton("↓");
        down.setContentDescription("Automatisch nach unten scrollen");
        down.setOnClickListener(v -> startAutoScroll(1));
        panel.addView(down, new LinearLayout.LayoutParams(dp(124), dp(54)));

        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        speedRow.setGravity(Gravity.CENTER);

        Button slower = makeButton("−");
        slower.setContentDescription("Langsamer");
        slower.setOnClickListener(v -> setSpeed(speed - 1));
        speedRow.addView(slower, new LinearLayout.LayoutParams(dp(38), dp(42)));

        speedLabel = new TextView(this);
        speedLabel.setTextColor(Color.WHITE);
        speedLabel.setTextSize(14f);
        speedLabel.setGravity(Gravity.CENTER);
        speedRow.addView(speedLabel, new LinearLayout.LayoutParams(dp(48), dp(42)));

        Button faster = makeButton("+");
        faster.setContentDescription("Schneller");
        faster.setOnClickListener(v -> setSpeed(speed + 1));
        speedRow.addView(faster, new LinearLayout.LayoutParams(dp(38), dp(42)));

        panel.addView(speedRow, new LinearLayout.LayoutParams(dp(124), dp(44)));

        stateLabel = new TextView(this);
        stateLabel.setTextColor(Color.LTGRAY);
        stateLabel.setTextSize(12f);
        stateLabel.setGravity(Gravity.CENTER);
        stateLabel.setPadding(0, dp(5), 0, 0);
        panel.addView(stateLabel, new LinearLayout.LayoutParams(dp(124), dp(30)));

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.END;
        overlayParams.x = dp(8);
        overlayParams.y = dp(160);

        enableDragging(handle);
        overlay = panel;
        updateLabels();
        windowManager.addView(overlay, overlayParams);
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15f);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(roundedBackground(Color.rgb(70, 70, 70), dp(9)));
        return button;
    }

    private void enableDragging(View handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            float downRawX;
            float downRawY;
            int startX;
            int startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (overlayParams == null || windowManager == null || overlay == null) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = overlayParams.x;
                        startY = overlayParams.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        overlayParams.x = Math.max(0, startX - Math.round(event.getRawX() - downRawX));
                        overlayParams.y = Math.max(0, startY + Math.round(event.getRawY() - downRawY));
                        windowManager.updateViewLayout(overlay, overlayParams);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void removeOverlay() {
        if (overlay != null && windowManager != null) {
            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {}
            overlay = null;
        }
    }

    private void startAutoScroll(int newDirection) {
        direction = newDirection < 0 ? -1 : 1;
        handler.removeCallbacks(scrollLoop);
        updateLabels();
        handler.post(scrollLoop);
    }

    private void stopAutoScroll() {
        direction = 0;
        handler.removeCallbacks(scrollLoop);
        updateLabels();
    }

    private void setSpeed(int newSpeed) {
        speed = Math.max(1, Math.min(5, newSpeed));
        updateLabels();
        if (direction != 0) {
            handler.removeCallbacks(scrollLoop);
            handler.post(scrollLoop);
        }
    }

    private void updateLabels() {
        if (speedLabel != null) speedLabel.setText(speed + "/5");
        if (stateLabel != null) {
            if (direction < 0) stateLabel.setText("▲ läuft");
            else if (direction > 0) stateLabel.setText("▼ läuft");
            else stateLabel.setText("gestoppt");
        }
    }

    private long intervalForSpeed() {
        switch (speed) {
            case 1: return 1000L;
            case 2: return 720L;
            case 3: return 500L;
            case 4: return 350L;
            default: return 240L;
        }
    }

    private void scrollOnce(int dir) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo scrollable = root == null ? null : findBestScrollable(root, dir);

        if (scrollable != null) {
            Rect bounds = new Rect();
            scrollable.getBoundsInScreen(bounds);
            if (performSwipe(dir, bounds)) return;

            int action = dir > 0
                    ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
            if (scrollable.performAction(action)) return;
        }

        performSwipe(dir, null);
    }

    private AccessibilityNodeInfo findBestScrollable(AccessibilityNodeInfo root, int dir) {
        if (root == null) return null;
        int action = dir > 0
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;

        AccessibilityNodeInfo best = null;
        int bestArea = -1;
        java.util.ArrayDeque<AccessibilityNodeInfo> queue = new java.util.ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node == null) continue;

            boolean supportsDirection = (node.getActions() & action) != 0;
            if (node.isScrollable() || supportsDirection) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                int area = Math.max(0, bounds.width()) * Math.max(0, bounds.height());
                if (supportsDirection) area += 1_000_000;
                if (area > bestArea) {
                    bestArea = area;
                    best = node;
                }
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(child);
            }
        }
        return best;
    }

    private boolean performSwipe(int dir, Rect preferredBounds) {
        if (gestureBusy) return false;

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        if (width <= 0 || height <= 0) return false;

        Rect bounds;
        if (preferredBounds != null && preferredBounds.width() > dp(80) && preferredBounds.height() > dp(120)) {
            bounds = new Rect(preferredBounds);
            bounds.intersect(0, 0, width, height);
        } else {
            bounds = new Rect(0, Math.round(height * 0.18f), width, Math.round(height * 0.82f));
        }

        if (bounds.width() <= 0 || bounds.height() <= 0) return false;

        float x = bounds.left + bounds.width() * 0.50f;
        float fraction;
        long duration;
        switch (speed) {
            case 1:
                fraction = 0.10f; duration = 430L; break;
            case 2:
                fraction = 0.14f; duration = 350L; break;
            case 3:
                fraction = 0.18f; duration = 280L; break;
            case 4:
                fraction = 0.23f; duration = 210L; break;
            default:
                fraction = 0.30f; duration = 160L; break;
        }

        float distance = Math.max(dp(48), bounds.height() * fraction);
        float centerY = bounds.top + bounds.height() * 0.52f;
        float startY = dir > 0 ? centerY + distance / 2f : centerY - distance / 2f;
        float endY = dir > 0 ? centerY - distance / 2f : centerY + distance / 2f;

        float margin = dp(16);
        startY = Math.max(bounds.top + margin, Math.min(bounds.bottom - margin, startY));
        endY = Math.max(bounds.top + margin, Math.min(bounds.bottom - margin, endY));

        Path path = new Path();
        path.moveTo(x, startY);
        path.lineTo(x, endY);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
                .build();

        gestureBusy = true;
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                gestureBusy = false;
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                gestureBusy = false;
            }
        }, handler);

        if (!accepted) gestureBusy = false;
        return accepted;
    }

    private GradientDrawable roundedBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
