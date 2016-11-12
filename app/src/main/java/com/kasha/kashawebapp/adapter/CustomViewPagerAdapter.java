package com.kasha.kashawebapp.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.fragments.HomeFragment;
import com.kasha.kashawebapp.fragments.LocationFragment;

/**
 * Created by rob on 11/10/16.
 */

public class CustomViewPagerAdapter extends FragmentStatePagerAdapter {
    private Context context;

    String[] tabsTitles;
    public CustomViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        Resources res = context.getResources();
        tabsTitles = res.getStringArray(R.array.tabs_titles_array);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new LocationFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return tabsTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabsTitles[position];
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

}