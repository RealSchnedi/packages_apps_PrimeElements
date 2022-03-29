/*
 * Copyright (C) 2022
 * SPDX-License-Identifier: Apache-2.0
*/

package com.prime.settings;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class TopLevelPrimeSettingsPreferenceController extends BasePreferenceController {

    public TopLevelPrimeSettingsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
