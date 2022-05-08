package lordkbx.workshop.ereader.reader;

import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;

public class ShowRunner implements Runnable {
    private ActionBar actionBar;
    private LinearLayout controlsView;

    public ShowRunner(ActionBar bar, LinearLayout layout){
        this.actionBar = bar;
        this.controlsView = layout;
    }

    @Override
    public void run() {
        // Delayed display of UI elements
        actionBar.show();
        controlsView.setVisibility(View.VISIBLE);
    }
}
