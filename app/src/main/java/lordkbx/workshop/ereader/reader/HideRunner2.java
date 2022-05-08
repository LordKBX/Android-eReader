package lordkbx.workshop.ereader.reader;

import android.view.View;
import android.webkit.WebView;

public class HideRunner2 implements Runnable {
    private WebView controlsView;

    public HideRunner2(WebView layout){
        this.controlsView = layout;
    }

    @Override
    public void run() {
        // Delayed removal of status and navigation bar
        controlsView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }
}
