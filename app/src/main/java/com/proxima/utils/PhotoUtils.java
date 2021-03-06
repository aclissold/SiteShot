package com.proxima.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.proxima.models.UserPhoto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

///
// Created by Andrew Clissold, Rachel Glomski, Jon Wong on 10/6/14.
// Class that contains utilities for uploading photos to parse
//
// Recent Version: 11/26/14
public class PhotoUtils {

    private static final String TAG = PhotoUtils.class.getName();
    private static PhotoUtils mInstance = null;

    private List<UserPhoto> mUserPhotos;
    private List<UserPhoto> mMarkerPhotos;
    private List<UserPhoto> mClusterPhotos;
    private String mCurrentPhotoPath;

    private PhotoUtils() {}

    // method to return an instance of PhotoUtils or create a new one
    public static PhotoUtils getInstance() {
        if (mInstance == null) {
            mInstance = new PhotoUtils();
        }
        return mInstance;
    }

    // method to download UserPhoto objects created by the current user
    public void downloadUserPhotos() {
        ParseQuery<UserPhoto> query = UserPhoto.getQuery();
        query.whereEqualTo("createdBy", ParseUser.getCurrentUser().getUsername());
        query.orderByDescending("createdAt");
        query.findInBackground(new FindCallback<UserPhoto>() {
            @Override
            public void done(List<UserPhoto> resultUserPhotos, ParseException e) {
                if (e == null) {
                    mUserPhotos = resultUserPhotos;
                } else {
                    Log.e(TAG, "error retrieving user photos:");
                    e.printStackTrace();
                }
            }
        });
    }

    // method to download photos for a marker thumbnail
    public void downloadMarkerPhotos() {
        ParseQuery<UserPhoto> query = UserPhoto.getQuery();
        query.orderByDescending("createdAt");
        query.findInBackground(new FindCallback<UserPhoto>() {
            @Override
            public void done(List<UserPhoto> resultUserPhotos, ParseException e) {
                if (e == null) {
                    mMarkerPhotos = resultUserPhotos;
                } else {
                    Log.e(TAG, "error retrieving user photos:");
                    e.printStackTrace();
                }
            }
        });
    }

    // method to download photos in a given cluster
    public void downloadClusterPhotos(String[] cluster) throws ParseException {
        List<String> test = Arrays.asList(cluster);
        ParseQuery<UserPhoto> clusterQuery = UserPhoto.getQuery();
        clusterQuery = clusterQuery.whereContainedIn("objectId", test);
        // TODO: make this an ordered query
        mClusterPhotos = clusterQuery.find();
    }

    // method to return downloaded UserPhotos
    public List<UserPhoto> getUserPhotos() {
        return mUserPhotos;
    }

    // method to return downloaded UserPhotos for a cluster
    public List<UserPhoto> getClusterPhotos(String[] cluster) throws ParseException {
        downloadClusterPhotos(cluster);
        return mClusterPhotos;
    }

    // method to return downloaded UserPhotos for a marker thumbnail
    public List<UserPhoto> updateMarkerPhotos() {
        downloadMarkerPhotos();
        return mMarkerPhotos;
    }

    // method to return the current photopath
    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    // method to upload a photo to Parse
    public Bitmap uploadPhoto(byte[] data, ParseGeoPoint geoPoint, String description,
                              boolean rotateFlag, boolean selfFLag, SaveCallback callback) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return uploadPhoto(bitmap, geoPoint, description, rotateFlag, selfFLag, callback);
    }

    ///
    // Convert an image taken by embedded camera to a bitmap and then upload the bitmap to
    // Parse
    //
    // @param data the image taken from embedded camera
    // @param rotateFlag flag to denote rotation correction
    //
    // @return       the image converted to bitmap uploaded to Parse
    //
    ///
    public Bitmap uploadPhoto(byte[] data, ParseGeoPoint geoPoint, boolean rotateFlag, boolean selfFlag) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return uploadPhoto(bitmap, geoPoint, null, rotateFlag, selfFlag);
    }

    ///
    // Upload an image Parse attributed to the current user
    //
    // @param bitmap the image to upload to Parse
    // @param rotateFlag flag to denote rotation correction
    //
    // @return       the same bitmap, rotated if necessary
    //
    ///
    public Bitmap uploadPhoto(Bitmap bitmap, ParseGeoPoint geoPoint, String description,
                              boolean rotateFlag, boolean selfFlag) {
        // Prepare the image data.
        Bitmap rotatedBitmap = rotate(bitmap, rotateFlag, selfFlag);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("photo.jpg", data);

        // Get the current user to pre-unlock the photo.
        String username = ParseUser.getCurrentUser().getUsername();
        ArrayList<String> unlocked = new ArrayList<String>();
        unlocked.add(username);

        // Create and save the ParseObject.
        ParseObject object = ParseObject.create("UserPhoto");
        object.put("photo", file);
        object.put("location", geoPoint);
        object.put("createdBy", username);
        object.put("unlocked", unlocked);
        if (description != null) { object.put("description", description); }
        object.saveInBackground();

        Tracker.getInstance().trackPhotoUpload(username);

        return rotatedBitmap;
    }

    // Overloaded version of the above for when a callback is necessary.
    public Bitmap uploadPhoto(Bitmap bitmap, ParseGeoPoint geoPoint, String description,
                              boolean rotateFlag, boolean selfFlag, SaveCallback callback) {
        // Prepare the image data.
        Bitmap rotatedBitmap = rotate(bitmap, rotateFlag, selfFlag);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("photo.jpg", data);

        // Get the current user to pre-unlock the photo.
        String username = ParseUser.getCurrentUser().getUsername();
        ArrayList<String> unlocked = new ArrayList<String>();
        unlocked.add(username);

        // Create and save the ParseObject.
        ParseObject object = ParseObject.create("UserPhoto");
        object.put("photo", file);
        object.put("location", geoPoint);
        object.put("createdBy", username);
        object.put("description", description);
        object.put("unlocked", unlocked);
        object.saveInBackground(callback);

        Tracker.getInstance().trackPhotoUpload(username);

        return rotatedBitmap;
    }

    ///
    // Upload an image to be used as user profile icon to Parse
    //
    // @param bitmap the image to upload to Parse
    //
    // @return       the same bitmap, rotated if necessary
    //
    ///
    public Bitmap uploadProfilePhoto(Bitmap bitmap, boolean profileRotate) {
        boolean selfRotate = false;
        Bitmap rotatedBitmap = rotate(bitmap, profileRotate, selfRotate);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("userIcon.jpg", data);
        ParseUser user = ParseUser.getCurrentUser();
        user.put("icon", file);
        user.saveInBackground();

        Tracker.getInstance().trackProfilePhotoUpload(ParseUser.getCurrentUser().getUsername());

        return rotatedBitmap;
    }

    public Bitmap rotatePreview(Bitmap bitmap, boolean shouldRotate, boolean selfRotate){
        return rotate (bitmap, shouldRotate, selfRotate);
    }

    // method to rotate the bitmap for upload if necessary
    private Bitmap rotate(Bitmap bitmap, boolean shouldRotate, boolean selfRotate) {
        boolean rotateFlag = shouldRotate;
        boolean selfFlag = selfRotate;
        int iconOrientation;
        // portrait upload by forcing 90 degree rotation of the bitmap when uploading from the
        // embedded camera
        if (rotateFlag && selfFlag) {
            iconOrientation = 8;
        }
        else if(rotateFlag){
            iconOrientation = 6;
        }
        else {
            iconOrientation = 1;
        }

        // if the upload is from native camera rotate the result using the headers added by the
        // native camera. if upload is from embedded camera force 90 degree rotation
        if (rotateFlag == false) {
            try {
                ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                iconOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        Matrix matrix = new Matrix();

        switch (iconOrientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    ///
    // Creates the photo file on disk, ready to be written to. After calling this method,
    // getCurrentPhotoPath will return the absolute path of the file.
    //
    // @return             a new photo file
    // @throws IOException
    ///
    public File createPhotoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
