package net.kzxiv.notify.client;

import android.app.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.preference.*;

public class ConfigurationActivity extends PreferenceActivity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
    {
        int NOTIFICATION_ID = 0;
        String CHANNEL_ID = "notifikator";

        Resources res = getResources();
        if (res.getString(R.string.key_send).equals(preference.getKey()))
        {
            NotificationManager mgr = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            Notification.Builder nb = new Notification.Builder(this);

            nb.setContentTitle(res.getString(R.string.notification_title));
            nb.setContentText(res.getString(R.string.notification_text));
            nb.setSmallIcon(R.drawable.mask);

            BitmapDrawable largeIconDrawable = (BitmapDrawable) res.getDrawable(R.drawable.icon);
            Bitmap largeIconBitmap = largeIconDrawable.getBitmap();
            nb.setLargeIcon(largeIconBitmap);

            // `VERSION_CODES.O` means SDK 26
            // Thanks Google, very readable
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // notification channel setup
                NotificationChannel mChannel = null;
                mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription("");
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.GREEN);
                mChannel.enableVibration(false);
                mgr.createNotificationChannel(mChannel);

                nb.setChannelId(CHANNEL_ID);
            }

            mgr.notify(NOTIFICATION_ID, nb.build());
            return false;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}