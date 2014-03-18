package de.mkoetter.radmon.util;

import android.preference.ListPreference;
import android.preference.Preference;

import java.text.MessageFormat;

/**
 * Created by Michael on 13.03.14.
 */
public class SummaryPreferenceChangeListener implements Preference.OnPreferenceChangeListener {
    private String pattern = null;
    private Preference.OnPreferenceChangeListener delegate = null;

    public SummaryPreferenceChangeListener() {
        this(null, null);
    }

    public SummaryPreferenceChangeListener(Preference.OnPreferenceChangeListener delegate, String pattern) {
        this.delegate = delegate;
        this.pattern = pattern;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        CharSequence label = null;
        CharSequence value = null;
        value = o.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            for (int i = 0; i < listPreference.getEntryValues().length; i++){
                if (o.equals(listPreference.getEntryValues()[i])) {
                    label = listPreference.getEntries()[i];
                    break;
                }
            }
        } else {
            label = value;
        }

        if (pattern != null) {
            preference.setSummary(MessageFormat.format(pattern, label, value));
        } else {
            preference.setSummary(label);
        }

        return delegate != null ? delegate.onPreferenceChange(preference, o) : true;
    }
}
