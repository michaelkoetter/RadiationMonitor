package de.mkoetter.radmon;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.mkoetter.radmon.fragment.CurrentFragment;
import de.mkoetter.radmon.fragment.LogsFragment;

/**
 * Created by mk on 02.04.14.
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new CurrentFragment();
            case 1:
                return new LogsFragment();
        };
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
