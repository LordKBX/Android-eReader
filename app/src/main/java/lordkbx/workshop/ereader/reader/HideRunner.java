package lordkbx.workshop.ereader.reader;

import android.app.Activity;

public class HideRunner implements Runnable {
    private ReaderActivity activity;

    public HideRunner(ReaderActivity parent){
        this.activity = parent;
    }

    @Override
    public void run() {
        this.activity.hide();
    }
}
