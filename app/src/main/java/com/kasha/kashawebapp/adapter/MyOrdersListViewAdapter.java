package com.kasha.kashawebapp.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kasha.kashawebapp.R;
import com.kasha.kashawebapp.helper.Util;

public class MyOrdersListViewAdapter extends ArrayAdapter<String>{

    private View theView;
    private LayoutInflater theInflater;
    private  String selectedOrder;
    private TextView theTextView;
    private ImageView theImageView;
    private Util util;

    public MyOrdersListViewAdapter(Context context, Cursor res) {
        super(context,R.layout.row_layout_2,R.id.row_text_view_2,
                Util.getStringArrayFromColumnCursor(res));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        theInflater = LayoutInflater.from(getContext());
        theView = theInflater.inflate(R.layout.row_layout_2, parent, false);
        selectedOrder = getItem(position);
        theTextView = (TextView) theView.findViewById(R.id.row_text_view_2);
        theTextView.setText(selectedOrder);
        theImageView = (ImageView) theView.findViewById(R.id.image_view_1);
        //theImageView.setImageResource(R.drawable.dot);

        return theView;
    }
}