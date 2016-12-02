package com.kasha.kashawebapp.fragments;

import android.content.Intent;
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
import com.kasha.kashawebapp.views.NotificationDisplayActivity;

/**
 * Created by rkabagamba on 11/28/2016.
 */

public class HistoryFragment extends Fragment {

    private View rootView;
    private KashaWebAppDBHelper mydb;
    private LayoutInflater layoutinflater;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_history, container, false);
        mydb = KashaWebAppDBHelper.getInstance(getContext());

        ListAdapter mOrdersArrayAdapter = new MyOrdersListViewAdapter(getContext(),
                                                                        mydb.getAllOrders());
        ListView ordersListView = (ListView) rootView.findViewById(R.id.main_list_view);

        // Add a header
        layoutinflater = getActivity().getLayoutInflater();
        ViewGroup header = (ViewGroup) layoutinflater.inflate(R.layout.listview_item_header,ordersListView,false);
        ordersListView.addHeaderView(header);

        ordersListView.setAdapter(mOrdersArrayAdapter);

        ordersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedOrder = String.valueOf(parent.getItemAtPosition(position));

                Toast.makeText(getActivity(),"Notifications for " +selectedOrder,
                                                    Toast.LENGTH_LONG).show();

                Intent mIntent = new Intent(getActivity(), NotificationDisplayActivity.class);
                mIntent.putExtra("order",selectedOrder);
                getActivity().startActivity(mIntent);

            }
        });

        return rootView;
    }
}
