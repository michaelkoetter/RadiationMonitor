package de.mkoetter.radmon;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.Locale;

import de.mkoetter.radmon.util.SummaryPreferenceChangeListener;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    public static final int ENABLE_BLUETOOTH = 0;
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.0#", DecimalFormatSymbols.getInstance(Locale.US));

    BluetoothAdapter bt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ListPreference prefConnectionType = (ListPreference)findPreference("connectionType");
        prefConnectionType.setSummary(prefConnectionType.getEntry());
        prefConnectionType.setOnPreferenceChangeListener(new SummaryPreferenceChangeListener(this, null));

        updateBluetoothDevicePreference(prefConnectionType.getValue().equals("BLUETOOTH"));

        ListPreference prefTube = (ListPreference)findPreference("tube");
        prefTube.setSummary(prefTube.getEntry());
        prefTube.setOnPreferenceChangeListener(new SummaryPreferenceChangeListener(this, null));


        updateConversionFactorPreference("CUSTOM".equals(prefTube.getValue()),
                getDefaultConversionFactor(prefTube.getValue()));

        EditTextPreference prefConversionFactor = (EditTextPreference)findPreference("conversionFactor");
        prefConversionFactor.setOnPreferenceChangeListener(new SummaryPreferenceChangeListener(this,
                getString(R.string.pref_conversion_factor_summary)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if ("connectionType".equals(preference.getKey())) {
            updateBluetoothDevicePreference("BLUETOOTH".equals(o));
        } else if ("tube".equals(preference.getKey())) {
            updateConversionFactorPreference("CUSTOM".equals(o), getDefaultConversionFactor((String) o));
        }
        return true;
    }



    private void updateBluetoothDevicePreference(boolean enable) {
        BluetoothDevicePreference bluetoothDevicePreference =
                (BluetoothDevicePreference)findPreference("bluetoothDevice");
        bluetoothDevicePreference.setEnabled(enable);
    }

    private void updateConversionFactorPreference(boolean enable, Number conversionFactor) {
        EditTextPreference conversionFactorPreference = (EditTextPreference) findPreference("conversionFactor");
        if (conversionFactor != null) {
            conversionFactorPreference.setText(DECIMAL_FORMAT.format(conversionFactor));
        }

        conversionFactorPreference.setSummary(MessageFormat.format(getString(R.string.pref_conversion_factor_summary),
                conversionFactorPreference.getText()));
        conversionFactorPreference.setEnabled(enable);
    }

    private Number getDefaultConversionFactor(String tube) {
        if ("SBM20".equals(tube)) {
            return 175.0;
        }

        return null;
    }
}
