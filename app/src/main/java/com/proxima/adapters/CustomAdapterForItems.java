package com.proxima.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.proxima.R;
import com.proxima.fragments.ProximaMapFragment;

/**
 * Custom Info Window Adapter for markers
 * Created by Jonathan on 11/2/2014.
 */
public class CustomAdapterForItems implements GoogleMap.InfoWindowAdapter {
    private final String TAG = getClass().getName();
    private ProximaMapFragment owner;
    public CustomAdapterForItems(ProximaMapFragment owner) {
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
        if (owner.unlockFlag) {
            ImageView imageView = (ImageView) view.findViewById(R.id.imageView2);
            ParseFile file = owner.mClickedClusterItem.getUserPhoto().getPhoto();
            Bitmap bitmap = null;
            try {
                byte[] data = file.getData();
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            } catch (ParseException e) {
                Log.e(TAG, "error getting user photo bytes");
                e.printStackTrace();
            }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
        else {
            ImageView imageView = (ImageView) view.findViewById(R.id.imageView2);
            imageView.setImageBitmap(BitmapFactory.decodeResource(view.getResources(), R.drawable.locked));
        }
        return view;
    }
}
