package de.mkoetter.radmon;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.mkoetter.radmon.fragment.CurrentFragment;
import de.mkoetter.radmon.fragment.SessionsFragment;

/**
 * Created by mk on 02.04.14.
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {

    String[] tabTitles;

    public TabsPagerAdapter(String[] tabTitles, FragmentManager fm) {
        super(fm);
        this.tabTitles = tabTitles;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new CurrentFragment();
            case 1:
                return new SessionsFragment();
        };
        return null;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
