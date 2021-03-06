package com.proxima.adapters;

import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.proxima.R;
import com.proxima.fragments.ProximaMapFragment;

/**
 * Custom Info Window Adapter for Clusters
 * Created by Jonathan on 11/2/2014.
 */
public class CustomAdapterForClusters implements GoogleMap.InfoWindowAdapter{
    private final String TAG = getClass().getName();
    private ProximaMapFragment owner;

    public CustomAdapterForClusters(ProximaMapFragment owner) {
        this.owner = owner;
    }
    @Override
    public View getInfoWindow(Marker marker) {
        // Use the default frame.
        return null;
    }
    @Override
    public View getInfoContents(Marker marker) {
        View view = owner.getActivity().getLayoutInflater().inflate(R.layout.info_window, null);

        ImageView imageView = (ImageView) view.findViewById(R.id.imageView2);
        imageView.setImageBitmap(BitmapFactory.decodeResource(view.getResources(), R.drawable.proxima_cluster));
        TextView txtV = (TextView) view.findViewById(R.id.textView);
        txtV.setText("Number of Photos: " + owner.mClusterSize);

        return view;
    }
}
