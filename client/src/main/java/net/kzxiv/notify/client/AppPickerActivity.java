package net.kzxiv.notify.client;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppPickerActivity extends Activity
{
    private List<AppEntry> allApps;
    private List<AppEntry> filteredApps;
    private Set<String> deniedPackages;
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        if (getActionBar() != null)
            getActionBar().setTitle(R.string.app_picker_title);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        deniedPackages = new HashSet<>(
            prefs.getStringSet(AppConstants.PACKAGE_DENY_LIST_PREF_KEY, new HashSet<String>())
        );

        loadInstalledApps();

        filteredApps = new ArrayList<>(allApps);
        adapter = new AppListAdapter();

        ListView listView = (ListView) findViewById(R.id.appListView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry entry = filteredApps.get(position);
            entry.denied = !entry.denied;
            if (entry.denied)
                deniedPackages.add(entry.packageName);
            else
                deniedPackages.remove(entry.packageName);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.appCheckBox);
            checkBox.setChecked(entry.denied);
        });

        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                filterApps(newText);
                return true;
            }
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveDenylist();
    }

    private void loadInstalledApps()
    {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
        allApps = new ArrayList<>();

        for (ApplicationInfo appInfo : installedApps)
        {
            AppEntry entry = new AppEntry();
            entry.appName = pm.getApplicationLabel(appInfo).toString();
            entry.packageName = appInfo.packageName;
            entry.icon = pm.getApplicationIcon(appInfo);
            entry.denied = deniedPackages.contains(appInfo.packageName);
            allApps.add(entry);
        }

        Collections.sort(allApps, (a, b) -> {
            // Denied apps first, then alphabetical
            if (a.denied != b.denied)
                return a.denied ? -1 : 1;
            return a.appName.compareToIgnoreCase(b.appName);
        });
    }

    private void filterApps(String query)
    {
        filteredApps.clear();
        if (query == null || query.isEmpty())
        {
            filteredApps.addAll(allApps);
        }
        else
        {
            String lowerQuery = query.toLowerCase();
            for (AppEntry entry : allApps)
            {
                if (entry.appName.toLowerCase().contains(lowerQuery)
                    || entry.packageName.toLowerCase().contains(lowerQuery))
                {
                    filteredApps.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveDenylist()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(AppConstants.PACKAGE_DENY_LIST_PREF_KEY, new HashSet<>(deniedPackages));
        editor.apply();
    }

    private static class AppEntry
    {
        String appName;
        String packageName;
        Drawable icon;
        boolean denied;
    }

    private class AppListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return filteredApps.size();
        }

        @Override
        public Object getItem(int position)
        {
            return filteredApps.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder;
            if (convertView == null)
            {
                convertView = LayoutInflater.from(AppPickerActivity.this)
                    .inflate(R.layout.app_list_item, parent, false);
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.appIcon);
                holder.appName = (TextView) convertView.findViewById(R.id.appName);
                holder.packageName = (TextView) convertView.findViewById(R.id.packageName);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.appCheckBox);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            AppEntry entry = filteredApps.get(position);
            holder.icon.setImageDrawable(entry.icon);
            holder.appName.setText(entry.appName);
            holder.packageName.setText(entry.packageName);
            holder.checkBox.setChecked(entry.denied);

            return convertView;
        }

        private class ViewHolder
        {
            ImageView icon;
            TextView appName;
            TextView packageName;
            CheckBox checkBox;
        }
    }
}
