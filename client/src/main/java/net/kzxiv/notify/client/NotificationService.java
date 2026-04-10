package net.kzxiv.notify.client;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class NotificationService extends NotificationListenerService
{
    private static final String TAG = AppConstants.TAG;

    public void onCreate()
    {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Log.d(TAG, "Notification service created.");
    }

    public void onDestroy()
    {
        Log.d(TAG, "Notification service destroyed.");
        super.onDestroy();
    }

    public void onNotificationPosted(StatusBarNotification sbn)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Set<String> packageDenylist = sharedPreferences.getStringSet(AppConstants.PACKAGE_DENY_LIST_PREF_KEY, new HashSet<>());

        String packageName = sbn.getPackageName();
        if (packageDenylist.contains(packageName)){
            Log.d(TAG, String.format("blocked notification for package \"%s\"", packageName));
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();

        final boolean enabled = prefs.getBoolean(res.getString(R.string.key_enabled), true);

        if (!enabled)
        {
            Log.i(TAG, "Skipping notification because not enabled.");
            return;
        }

        final boolean wifiOnly = prefs.getBoolean(res.getString(R.string.key_wifionly), false);

        if (wifiOnly)
        {
            ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = conn.getActiveNetworkInfo();

            if (ni == null || ni.getType() != ConnectivityManager.TYPE_WIFI)
            {
                Log.i(TAG, "Skipping notification because not connected to wifi.");
                return;
            }
        }

        final String endpointUrl = prefs.getString(res.getString(R.string.key_endpointurl), null);

        if (endpointUrl == null || "".equals(endpointUrl))
        {
            Log.e(TAG, "No endpoint specified.");
            return;
        }

        final boolean endpointAuth = prefs.getBoolean(res.getString(R.string.key_endpointauth), false);
        final String endpointUsername = prefs.getString(res.getString(R.string.key_endpointuser), null);
        final String endpointPassword = prefs.getString(res.getString(R.string.key_endpointpw), null);
        final String bodyTemplate = prefs.getString(res.getString(R.string.key_body_template),
                res.getString(R.string.default_body_template));
        final String headersJson = prefs.getString(res.getString(R.string.key_headers),
                res.getString(R.string.default_headers));

        Notification notification = sbn.getNotification();

        String[] substituted = applyPlaceholders(packageName, notification, bodyTemplate, headersJson);

        if (substituted == null)
        {
            Log.e(TAG, "Failed to build payload from template.");
            return;
        }

        Intent i = new Intent(this, HttpTransportService.class);
        i.putExtra(HttpTransportService.EXTRA_URL, endpointUrl);
        i.putExtra(HttpTransportService.EXTRA_AUTH, endpointAuth);
        i.putExtra(HttpTransportService.EXTRA_HEADERS, substituted[1]);
        if (endpointAuth)
        {
            i.putExtra(HttpTransportService.EXTRA_USERNAME, endpointUsername);
            i.putExtra(HttpTransportService.EXTRA_PASSWORD, endpointPassword);
        }

        i.putExtra(HttpTransportService.EXTRA_PAYLOAD_TYPE, "application/json");
        i.putExtra(HttpTransportService.EXTRA_PAYLOAD, substituted[0].getBytes());

        startService(i);
    }

    public void onNotificationRemoved(StatusBarNotification sbn)
    {
    }

    /**
     * Substitutes %placeholders% in both body template and headers.
     * Returns {body, headers} with placeholders replaced, or null if notification data is missing.
     */
    private String[] applyPlaceholders(String packageName, Notification notification,
                                       String bodyTemplate, String headersTemplate)
    {
        final String title = notification.extras.getString(Notification.EXTRA_TITLE);
        final String text = notification.extras.getString(Notification.EXTRA_TEXT);

        if (title == null || text == null)
            return null;

        final String app = getApplicationName(packageName);
        final Bitmap iconLg = getLargeIcon(notification);
        final Bitmap iconSm = BitmapHelper.getPackageIcon(this, packageName,
                notification.extras.getInt(Notification.EXTRA_SMALL_ICON));

        final String iconUri = iconLg != null
                ? BitmapHelper.getDataUri(BitmapHelper.ensureSize(iconLg, 192, 192)) : "";
        final String badgeUri = iconSm != null
                ? BitmapHelper.getDataUri(BitmapHelper.ensureSize(iconSm, 72, 72)) : "";
        final int displayTime = determineDisplayTime(title, text);

        String body = substituteAll(bodyTemplate, title, text, packageName, app, iconUri, badgeUri, displayTime);
        String headers = substituteAll(headersTemplate, title, text, packageName, app, iconUri, badgeUri, displayTime);

        return new String[] { body, headers };
    }

    private static String substituteAll(String template, String title, String text,
                                        String packageName, String app, String iconUri,
                                        String badgeUri, int displayTime)
    {
        return template
                .replace("%title%", escapeJson(title))
                .replace("%text%", escapeJson(text))
                .replace("%package%", escapeJson(packageName))
                .replace("%app%", escapeJson(app))
                .replace("%icon%", escapeJson(iconUri))
                .replace("%badge%", escapeJson(badgeUri))
                .replace("%displaytime%", Integer.toString(displayTime));
    }

    private static String escapeJson(String value)
    {
        // Android's JSONObject.quote() escapes '/' as '\/' which corrupts base64 data.
        // Un-escape forward slashes since they don't require escaping in JSON (RFC 8259).
        String quoted = JSONObject.quote(value);
        return quoted.substring(1, quoted.length() - 1).replace("\\/", "/");
    }

    private Bitmap getLargeIcon(Notification notification)
    {
        // On API 23+, EXTRA_LARGE_ICON can be an Icon instead of a Bitmap
        Object raw = notification.extras.get(Notification.EXTRA_LARGE_ICON);
        if (raw instanceof Bitmap)
            return (Bitmap) raw;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            Icon icon = notification.getLargeIcon();
            if (icon != null)
            {
                Drawable drawable = icon.loadDrawable(this);
                if (drawable instanceof BitmapDrawable)
                    return ((BitmapDrawable) drawable).getBitmap();
            }
        }

        return null;
    }

    private String getApplicationName(String packageName)
    {
        PackageManager pkg = getPackageManager();
        try
        {
            ApplicationInfo info = pkg.getApplicationInfo(packageName, 0);
            return pkg.getApplicationLabel(info).toString();
        }
        catch (PackageManager.NameNotFoundException ex)
        {
            return packageName;
        }
    }

    private static int determineDisplayTime(String title, String text)
    {
        final int rawTime = ((title.length() + text.length()) * 1000) / 5;
        return Math.max(5000, rawTime);
    }
}
