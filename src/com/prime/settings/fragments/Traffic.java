/*
 * Copyright (C) 2022
 * SPDX-License-Identifier: Apache-2.0
*/
package com.prime.settings.fragments;

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;

import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import com.prime.support.preferences.CustomSeekBarPreference;
import com.prime.support.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class Traffic extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String NETWORK_TRAFFIC_TYPE  = "network_traffic_type";
    private static final String NETWORK_TRAFFIC_VIEW_LOCATION  = "network_traffic_view_location";
    private static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD  = "network_traffic_autohide_threshold";
    private static final String NETWORK_TRAFFIC_ARROW  = "network_traffic_arrow";
    private static final String NETWORK_TRAFFIC_FONT_SIZE  = "network_traffic_font_size";

    private ListPreference mNetTrafficLocation;
    private ListPreference mNetTrafficType;
    private CustomSeekBarPreference mNetTrafficSize;
    private CustomSeekBarPreference mThreshold;
    private SystemSettingSwitchPreference mShowArrows;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.traffic);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefSet = getPreferenceScreen();

        int NetTrafficSize = Settings.System.getInt(resolver,
                Settings.System.NETWORK_TRAFFIC_FONT_SIZE, 36);
        mNetTrafficSize = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_FONT_SIZE);
        mNetTrafficSize.setValue(NetTrafficSize / 1);
        mNetTrafficSize.setOnPreferenceChangeListener(this);

        int type = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_TYPE, 0, UserHandle.USER_CURRENT);
        mNetTrafficType = (ListPreference) findPreference(NETWORK_TRAFFIC_TYPE);
        mNetTrafficType.setValue(String.valueOf(type));
        mNetTrafficType.setSummary(mNetTrafficType.getEntry());
        mNetTrafficType.setOnPreferenceChangeListener(this);

        mNetTrafficLocation = (ListPreference) findPreference(NETWORK_TRAFFIC_VIEW_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, 0, UserHandle.USER_CURRENT);
        mNetTrafficLocation.setOnPreferenceChangeListener(this);

        int trafvalue = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1, UserHandle.USER_CURRENT);
        mThreshold = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
        mThreshold.setValue(trafvalue);
        mThreshold.setOnPreferenceChangeListener(this);
        mShowArrows = (SystemSettingSwitchPreference) findPreference(NETWORK_TRAFFIC_ARROW);

        int netMonitorEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
        if (netMonitorEnabled == 1) {
            mNetTrafficLocation.setValue(String.valueOf(location+1));
            updateTrafficLocation(location+1);
        } else {
            mNetTrafficLocation.setValue("0");
            updateTrafficLocation(0);
        }
        mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntry());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.PRIME_ELEMENTS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNetTrafficLocation) {
            int location = Integer.valueOf((String) objValue);
            int index = mNetTrafficLocation.findIndexOfValue((String) objValue);
            mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntries()[index]);
            if (location > 0) {
                // Convert the selected location mode from our list {0,1,2} and store it to "view location" setting: 0=sb; 1=expanded sb
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, location-1, UserHandle.USER_CURRENT);
                // And also enable the net monitor
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 1, UserHandle.USER_CURRENT);
                updateTrafficLocation(location+1);
            } else { // Disable net monitor completely
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
                updateTrafficLocation(location);
            }
            return true;
        } else if (preference == mThreshold) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mNetTrafficType) {
            int val = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_TYPE, val,
                    UserHandle.USER_CURRENT);
            int index = mNetTrafficType.findIndexOfValue((String) objValue);
            mNetTrafficType.setSummary(mNetTrafficType.getEntries()[index]);
            return true;
        }  else if (preference == mNetTrafficSize) {
            int width = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_FONT_SIZE, width);
            return true;
        }
        return false;
    }

    public void updateTrafficLocation(int location) {
        if (location == 0) {
            mThreshold.setEnabled(false);
            mShowArrows.setEnabled(false);
            mNetTrafficType.setEnabled(false);
            mNetTrafficSize.setEnabled(false);
        } else {
            mThreshold.setEnabled(true);
            mShowArrows.setEnabled(true);
            mNetTrafficType.setEnabled(true);
            mNetTrafficSize.setEnabled(true);
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.traffic);
}
