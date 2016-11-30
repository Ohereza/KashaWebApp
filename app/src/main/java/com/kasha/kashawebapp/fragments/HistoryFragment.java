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
import com.kasha.kashawebapp.adapter.MessageListAdapter;
import com.kasha.kashawebapp.adapter.MyOrdersListViewAdapter;
import com.kasha.kashawebapp.helper.Msg;
import com.kasha.kashawebapp.helper.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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

                // Start the chatview
                ListView listMsg;
                listMsg = (ListView) getActivity().findViewById(R.id.main_list_view);
                ArrayList<Msg> listMessages;
                MessageListAdapter adapter;
                listMessages = new ArrayList<Msg>();

                // Msg(String cle, String email, String message, String attach, boolean fromMe, String teleAttach, String hour, String date)
                LinkedHashMap<String,String> notifications = Util.getMapFromCursor(
                                                mydb.getAllNotificationsToAnOrder(selectedOrder));

                for(String timestamp:notifications.keySet()){

                    listMessages.add(new Msg("", "", notifications.get(timestamp), "", false, "", "", timestamp ));
                }
/*                for (int i=0; i<notifications.size();i++) {
                    listMessages.add(new Msg("", "", notifications.get(i), "", false, "", "", "timestamp" ));
                }*/
                adapter = new MessageListAdapter(getContext(), listMessages);
                listMsg.setAdapter(adapter);

                Toast.makeText(getActivity(),"You have picked "+selectedOrder,Toast.LENGTH_LONG).show();
            }
        });

        return rootView;
    }
}
