package net.kzxiv.notify.client;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.net.*;
import android.preference.*;
import android.service.notification.*;
import android.util.*;

public class NotificationService extends NotificationListenerService
{
	public void onCreate()
	{
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		Log.d("Notifikator", "Notification service created.");
	}

	public void onDestroy()
	{
		Log.d("Notifikator", "Notification service destroyed.");
		super.onDestroy();
	}

	public void onNotificationPosted(StatusBarNotification sbn)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();

		final boolean enabled = prefs.getBoolean(res.getString(R.string.key_enabled), true);

		if (!enabled)
		{
			Log.i("Notifikator", "Skipping notification because not enabled.");
			return;
		}

		final boolean wifiOnly = prefs.getBoolean(res.getString(R.string.key_wifionly), false);

		if (wifiOnly)
		{
			ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = conn.getActiveNetworkInfo();

			if (ni == null || ni.getType() != ConnectivityManager.TYPE_WIFI)
			{
				Log.i("Notifikator", "Skipping notification because not connected to wifi.");
				return;
			}
		}

		final String protocol = prefs.getString(res.getString(R.string.key_protocol), null);
		final String endpointUrl = prefs.getString(res.getString(R.string.key_endpointurl), null);

		if (endpointUrl == null || "".equals(endpointUrl))
		{
			Log.e("Notifikator", "No endpoint specified.");
			return;
		}

		final boolean endpointAuth = prefs.getBoolean(res.getString(R.string.key_endpointauth), false);
		final String endpointUsername = prefs.getString(res.getString(R.string.key_endpointuser), null);
		final String endpointPassword = prefs.getString(res.getString(R.string.key_endpointpw), null);

		Notification notification = sbn.getNotification();

		Object[] payload;
		if (res.getString(R.string.protocol_kodi).equals(protocol))
			payload = getPayloadKodi(notification);
		else if (res.getString(R.string.protocol_kodi_addon).equals(protocol))
			payload = getPayloadKodiAddon(notification);
		else if (res.getString(R.string.protocol_adtv).equals(protocol))
			payload = getPayloadAdtv(notification);
		else if (res.getString(R.string.protocol_json).equals(protocol))
			payload = getPayloadJson(notification);
		else
			payload = null;

		if (payload == null)
		{
			Log.e("Notifikator", String.format("No payload or unknown protocol \"%s\".", protocol));
			return;
		}

		Intent i = new Intent(this, HttpTransportService.class);
		i.putExtra(HttpTransportService.EXTRA_URL, endpointUrl);
		i.putExtra(HttpTransportService.EXTRA_AUTH, endpointAuth);
		if (endpointAuth)
		{
			i.putExtra(HttpTransportService.EXTRA_USERNAME, endpointUsername);
			i.putExtra(HttpTransportService.EXTRA_PASSWORD, endpointPassword);
		}

		i.putExtra(HttpTransportService.EXTRA_PAYLOAD_TYPE, (String)payload[0]);
		i.putExtra(HttpTransportService.EXTRA_PAYLOAD, (byte[])payload[1]);

		startService(i);
	}

	public void onNotificationRemoved(StatusBarNotification sbn)
	{
	}

	private final Object[] getPayloadKodi(Notification notification)
	{
		// TODO:
		return null;
	}

	private final Object[] getPayloadKodiAddon(Notification notification)
	{
		// TODO:
		return null;
	}

	private final Object[] getPayloadAdtv(Notification notification)
	{
		// TODO:
		return null;
	}

	private final Object[] getPayloadJson(Notification notification)
	{
		// TODO:
		return new Object[] { "application/json", "{}".getBytes() };
	}
}
