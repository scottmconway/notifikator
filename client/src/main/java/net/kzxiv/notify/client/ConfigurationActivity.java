package net.kzxiv.notify.client;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class ConfigurationActivity extends PreferenceActivity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        Preference manageDenylistButton = findPreference(getString(R.string.key_manage_denylist));
        manageDenylistButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(ConfigurationActivity.this, AppPickerActivity.class));
                return true;
            }
        });

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
