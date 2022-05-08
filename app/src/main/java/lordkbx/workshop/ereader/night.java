package lordkbx.workshop.ereader;

import android.content.Context;
import android.content.res.Configuration;

public class night {
    public static boolean isNight(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            return true;
        }
        return false;
    }
}
