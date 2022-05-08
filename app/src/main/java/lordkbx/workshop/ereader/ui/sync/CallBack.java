package lordkbx.workshop.ereader.ui.sync;

public interface CallBack {
    void run(String body, String header);
    void error(String error);
}
