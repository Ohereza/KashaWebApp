package com.kasha.kashawebapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.kasha.kashawebapp.DB.KashaWebAppDBHelper;
import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.adapter.MyOrdersListViewAdapter;

/**
 * Created by rkabagamba on 11/28/2016.
 */

public class HistoryFragment extends Fragment {

    private View rootView;
    private KashaWebAppDBHelper mydb;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_history, container, false);

        String[] favoriteTVShows = {"Ellen Degeneres", "Prince of Bel-Air","Buffy: the vampire slayer",
                "Angel","Scandal","Police Academy"};

        ListAdapter mOrdersArrayAdapter = new MyOrdersListViewAdapter(getContext(), favoriteTVShows);
        ListView ordersListView = (ListView) rootView.findViewById(R.id.main_list_view);

        ordersListView.setAdapter(mOrdersArrayAdapter);

        ordersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedOrder = String.valueOf(parent.getItemAtPosition(position));
                Toast.makeText(getActivity(),"You have picked "+selectedOrder,Toast.LENGTH_LONG).show();
            }
        });

        return rootView;
    }
}
