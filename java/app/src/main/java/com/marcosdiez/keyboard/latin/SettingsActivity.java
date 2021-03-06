/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.marcosdiez.keyboard.latin;

import android.content.Intent;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
    private static final String DEFAULT_FRAGMENT = Settings.class.getName();

    @Override
    public Intent getIntent() {
        final Intent intent = super.getIntent();
        if (!intent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT);
        }
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

   private final String[] FRAGMENTS = {
            "com.marcosdiez.keyboard.latin.Settings",
            "com.marcosdiez.keyboard.latin.AdditionalSubtypeSettings"
    };

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d("IsValidFragment", fragmentName);
        for (String FRAGMENT : FRAGMENTS) {
            if (FRAGMENT.equals(fragmentName)) {
                return true;
            }
        }
        return super.isValidFragment(fragmentName);
    }

}
