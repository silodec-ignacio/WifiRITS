//andr
package com.silodec.wifirits;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.silodec.wifirits.model.RitsData;

import java.util.List;


public class RitsDataAdapter extends ArrayAdapter<RitsData> {

    List<RitsData> mRitsData;
    LayoutInflater mInflator;

    public RitsDataAdapter(@NonNull Context context, @NonNull List<RitsData> objects) {
        super(context, R.layout.list_rits_wifi, objects);

        mRitsData = objects;
        mInflator = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.list_rits_wifi, parent, false);
        }

        TextView tvRitsSSID = convertView.findViewById(R.id.listRitsSSID);
        TextView tvRitsName = convertView.findViewById(R.id.listRitsName);
        ImageView imageView = convertView.findViewById(R.id.listRitsImage);

        RitsData rits = mRitsData.get(position);

        tvRitsSSID.setText(rits.getFriendlyRitsSSID());
        tvRitsName.setText(rits.getRitsDriverName() + " " + rits.getRitsTruckNumber());
        imageView.setImageResource(R.drawable.wifi_rits);

        return convertView;
    }
}
