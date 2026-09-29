package de.chat.autoscroller;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private View buildUi() {
        int pad = dp(24);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(245, 245, 245));

        TextView title = new TextView(this);
        title.setText("Chat AutoScroller");
        title.setTextSize(28f);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView explainer = new TextView(this);
        explainer.setText("Scrollt den aktuell sichtbaren Chat automatisch. Nach Aktivierung erscheint eine kleine schwebende Steuerung über anderen Apps.");
        explainer.setTextSize(17f);
        explainer.setTextColor(Color.DKGRAY);
        explainer.setPadding(0, dp(18), 0, dp(18));
        root.addView(explainer);

        status = new TextView(this);
        status.setTextSize(18f);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button settingsButton = new Button(this);
        settingsButton.setText("1. Bedienungshilfe aktivieren");
        settingsButton.setAllCaps(false);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        root.addView(settingsButton, buttonParams());

        Button showButton = new Button(this);
        showButton.setText("2. Steuerung anzeigen");
        showButton.setAllCaps(false);
        showButton.setOnClickListener(v -> {
            ChatScrollAccessibilityService service = ChatScrollAccessibilityService.getInstance();
            if (service != null) {
                service.showOverlay();
            } else {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        root.addView(showButton, buttonParams());

        TextView note = new TextView(this);
        note.setText("Benutzung: Chat öffnen → ↓ für automatisches Weiter-Scrollen, ↑ für zurück, ■ zum Stoppen. Tempo mit − / + ändern. Die Steuerung lässt sich am oberen Griff verschieben.");
        note.setTextSize(16f);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note);

        return root;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        p.topMargin = dp(12);
        return p;
    }

    private void refreshStatus() {
        boolean enabled = isServiceEnabled(this, ChatScrollAccessibilityService.class.getName());
        if (enabled) {
            status.setText("✓ Dienst aktiv");
            status.setTextColor(Color.rgb(0, 110, 55));
        } else {
            status.setText("Dienst noch nicht aktiviert");
            status.setTextColor(Color.rgb(160, 40, 40));
        }
    }

    private static boolean isServiceEnabled(Context context, String className) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        String packageName = context.getPackageName();
        for (AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null) continue;
            String enabledPackage = info.getResolveInfo().serviceInfo.packageName;
            String enabledName = info.getResolveInfo().serviceInfo.name;
            if (packageName.equals(enabledPackage) && (className.equals(enabledName) || enabledName.endsWith(".ChatScrollAccessibilityService"))) {
                return true;
            }
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
