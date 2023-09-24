package net.kzxiv.notify.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationActivity extends PreferenceActivity
{
    private static final int PICK_FILE_REQUEST_CODE = 123;
    private static final Pattern PACKAGE_NAME_REGEX = Pattern.compile("^[a-z]+(\\.[a-z0-9_]+)*$");

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);


        Preference chooseFileButton = findPreference(getString(R.string.key_choose_denylist));
        chooseFileButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFilePickerForDenylist();
                return true;
            }
        });

    }

    private void openFilePickerForDenylist() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedFileUri = data.getData();
                readDenylistFile(selectedFileUri);
            }
        }
    }

    private void readDenylistFile(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            HashSet<String> lines = new HashSet<String>();
            String line;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = PACKAGE_NAME_REGEX.matcher(line);
                if (matcher.matches()) {
                    lines.add(line);
                }
                else {
                    // TODO parsing error - should we ignore this package name, or stop parsing and err out?
                    // For now we'll just ignore specific entries

                    // TODO should source error text from res/values
                    Toast.makeText(getApplicationContext(), String.format("Invalid package name for deny list - \"%s\"", line), Toast.LENGTH_SHORT).show();
                }
            }
            reader.close();
            inputStream.close();

            // save result in stored preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putStringSet(AppConstants.PACKAGE_DENY_LIST_PREF_KEY, lines);
            editor.apply();

        } catch (IOException e) {
            e.printStackTrace();
        }
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