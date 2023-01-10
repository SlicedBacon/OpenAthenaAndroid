// MainActivity.java
// Bobby Krupczak, rdk@krupczak.org, Matthew Krupczak, mwk@krupzak.org, et. al

// main activity; launch everything from here

// we need to figure out how to go back and forth between activities
// via our menu w/o forcing destroy and create
// Do this by adding flag to newly created intent which tells
// android to use existing activity rather than create new on
// if possible; otherwise create new activity
// intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

package com.openathena;

// import veraPDF fork of Adobe XMP core Java v5.1.0
import com.adobe.xmp.XMPError;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Html;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;

// Libraries from the U.S. National Geospatial Intelligence Agency https://www.nga.mil
import mil.nga.mgrs.grid.GridType;
import mil.nga.tiff.util.TiffException;
import mil.nga.mgrs.*;
import mil.nga.grid.features.Point;


public class MainActivity extends AthenaActivity {
    public static String TAG = MainActivity.class.getSimpleName();
    public final static String PREFS_NAME = "openathena.preferences";
    public final static String LOG_NAME = "openathena.log";
    public static int requestNo = 0;
    public static int dangerousAutelAwarenessCount;

    TextView textView;
    ImageView iView;

    ProgressBar progressBar;

    Button buttonSelectDEM;
    Button buttonSelectImage;
    Button buttonCalculate;

    protected String versionName;
    Uri imageUri = null;
    boolean isImageLoaded;
    Uri demUri = null;
    boolean isDEMLoaded;

    GeoTIFFParser theParser = null;
    TargetGetter theTGetter = null;

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        // Handle the returned Uri
                        //appendText("Back from chooser\n");

                        if (uri == null)
                            return;

                        //appendText("Back from chooser\n");
                        Log.d(TAG,"back from chooser for image");
                        imageSelected(uri);
                    }
                });

    ActivityResultLauncher<String> mGetDEM = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    //appendText("Back from chooser\n");

                    if (uri == null)
                        return;

                    //appendText("Back from chooser\n");
                    Log.d(TAG,"back from chooser for DEM");
                    demSelected(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate started");

        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);

        radioGroup = null;

        progressBar = (ProgressBar)  findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        buttonSelectDEM = (Button) findViewById(R.id.selectDEMButton); // ⛰
        buttonSelectImage = (Button) findViewById(R.id.selectImageButton); // ð
        buttonCalculate = (Button) findViewById(R.id.calculateButton); // ð
        setButtonReady(buttonSelectDEM, true);
        setButtonReady(buttonSelectImage, false);
        setButtonReady(buttonCalculate, false);

        dangerousAutelAwarenessCount = 0;
        isImageLoaded = false;
        isDEMLoaded = false;

        // get our prefs that we have saved

        textView = (TextView)findViewById(R.id.textView);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textViewTargetCoord = (TextView)findViewById(R.id.textViewTargetCoord);
        textViewTargetCoord.setMovementMethod(LinkMovementMethod.getInstance());

        iView = (ImageView)findViewById(R.id.imageView);

        // try to get our version out of app/build.gradle
        // versionName field
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
            Log.d(TAG, "Got version " + versionName);
        }
        catch (Exception e) {
            versionName = "unknown";
        }

        // check for saved state

        // open logfile for logging?  No, only open when someone calls
        // append

        clearText();
        appendLog("OpenAthena™ for Android version "+versionName+"\nMatthew Krupczak, Bobby Krupczak, et al.\n GPL-3.0, some rights reserved\n");

        if (savedInstanceState != null) {
            CharSequence textRestore = savedInstanceState.getCharSequence("textview");
            if (textRestore != null) {
                textView.setText(textRestore);
            }
            CharSequence textViewTargetCoordRestore = savedInstanceState.getCharSequence("textViewTargetCoord");
            if (textViewTargetCoordRestore != null) {
                textViewTargetCoord.setText(textViewTargetCoordRestore);
            }
            String storedUriString = savedInstanceState.getString("imageUri");
            if (storedUriString != null) {
                imageUri = Uri.parse(storedUriString);
                isImageLoaded = true;
                iView.setImageURI(imageUri);
            }
            String storedDEMUriString = savedInstanceState.getString("demUri");
            Log.d(TAG, "loaded demUri: " + storedDEMUriString);
            if (storedDEMUriString != null) {
                demUri = Uri.parse(storedDEMUriString);
                demSelected(demUri);
            }
            isTargetCoordDisplayed = savedInstanceState.getBoolean("isTargetCoordDisplayed");
        }

        restorePrefOutputMode(); // restore the outputMode from persistent settings
    }

    @Override
    protected void onSaveInstanceState(Bundle saveInstanceState) {
        Log.d(TAG,"onSaveInstanceState started");
        super.onSaveInstanceState(saveInstanceState);
        if (textView != null) {
            saveInstanceState.putCharSequence("textview", textView.getText());
        }
        if (textViewTargetCoord != null) {
            saveInstanceState.putCharSequence("textViewTargetCoord", textViewTargetCoord.getText());
        }
        saveInstanceState.putBoolean("isTargetCoordDisplayed", isTargetCoordDisplayed);
        if (imageUri != null) {
            saveInstanceState.putString("imageUri", imageUri.toString());
        }
        if (demUri != null) {
            Log.d(TAG, "saved demUri: " + demUri.toString());
            saveInstanceState.putString("demUri", demUri.toString());
        }
    }

    public void setButtonReady(Button aButton, boolean isItReady) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float enabled = 1.0f;
                float disabled = 0.5f;

                if (isItReady) {
                    aButton.setAlpha(enabled);
                    aButton.setClickable(true);
                } else {
                    aButton.setAlpha(disabled);
                    aButton.setClickable(false);
                }
            }
        });
    }

//    // stolen from InterWebs
//    // https://mobikul.com/pick-image-gallery-android/
//    private String getPathFromURI(Uri uri)
//    {
//        String res = null;
//        String[] proj = {MediaStore.Images.Media.DATA};
//        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
//        if (cursor.moveToFirst()) {
//            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            res = cursor.getString(column_index);
//        }
//        cursor.close();
//        return res;
//    }

//    // stolen from https://stackoverflow.com/questions/5568874/how-to-extract-the-file-name-from-uri-returned-from-intent-action-get-content
//    // modified by rdk
//    @SuppressLint("Range")
//    private String getFileName(Uri uri) {
//        String result = null;
//        if (uri.getScheme().equals("content")) {
//            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
//            try {
//                if (cursor != null && cursor.moveToFirst()) {
//                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
//                }
//            } finally {
//                cursor.close();
//            }
//            appendText("getFileName: using cursor thingys\n");
//        }
//        if (result == null) {
//            appendText("getFileName: using uri path\n");
//            result = uri.getPath();
//
//            //int cut = result.lastIndexOf('/');
//            //if (cut != -1) {
//              //  result = result.substring(cut + 1);
//            //}
//        }
//        return result;
//    }

    // back from image selection dialog; handle it
    private void imageSelected(Uri uri)
    {
        // save uri for later calculation
        if (imageUri != null && !uri.equals(imageUri)) {
            clearText(); // clear attributes textView
            isTargetCoordDisplayed = false;
            restorePrefOutputMode(); // reset textViewTargetCoord to mode descriptor

            isImageLoaded = false;
        }
        imageUri = uri;

        //appendText("imageSelected: uri is "+uri+"\n");
        //appendText(uri.toString()+"\n");

        //Log.d(TAG,"imageSelected: uri is "+uri);
        //aPath = getPathFromURI(uri);
        //Log.d(TAG,"imageSelected: path is "+aPath);

        AssetFileDescriptor fileDescriptor;
        try {
            fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(uri , "r");
        } catch(FileNotFoundException e) {
            imageUri = null;
            return;
        }

        long filesize = fileDescriptor.getLength();
        Log.d(TAG, "filesize: " + filesize);
        if (filesize < 1024 * 1024 * 20) { // check if filesize below 20Mb
            iView.setImageURI(uri);
        }  else { // otherwise:
            Toast.makeText(MainActivity.this, getString(R.string.image_is_too_large_error_msg), Toast.LENGTH_SHORT).show();
            iView.setImageResource(R.drawable.athena); // put up placeholder icon
        }

        appendLog("Selected image "+imageUri+"\n");
        appendText(getString(R.string.image_selected_msg));

        isImageLoaded = true;
        setButtonReady(buttonCalculate, true);
    }

    private void demSelected(Uri uri) {
        appendLog("Selected DEM " + uri + "\n");

        isDEMLoaded = false;
        setButtonReady(buttonSelectDEM, false);
        setButtonReady(buttonCalculate, false);

        Toast.makeText(MainActivity.this, getString(R.string.loading_geotiff_toast_msg), Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.VISIBLE);

        Handler myHandler = new Handler();

        // Load GeoTIFF in a new thread, this is a long-running task
        new Thread(new Runnable() { // Holy mother of Java
            @Override
            public void run() {
                Exception e = loadDEMnewThread(uri);
                myHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (e == null) {
                            String successOutput = "GeoTIFF DEM ";
//            successOutput += "\"" + uri.getLastPathSegment(); + "\" ";
                            successOutput += getString(R.string.dem_loaded_size_is_msg) + " " + theParser.getNumCols() + "x" + theParser.getNumRows() + "\n";
                            if (!outputModeIsSlavic()) {
                                successOutput += roundDouble(theParser.getMinLat()) + " ≤ lat ≤ " + roundDouble(theParser.getMaxLat()) + "\n";
                                successOutput += roundDouble(theParser.getMinLon()) + " ≤ lon ≤ " + roundDouble(theParser.getMaxLon()) + "\n\n";
                            } else {
                                try {
                                    // Believe me, I don't like this either....
                                    successOutput += roundDouble(CoordTranslator.toCK42Lat(theParser.getMinLat(), theParser.getMinLon(), theParser.getAltFromLatLon(theParser.getMinLat(), theParser.getMinLon()))) + " ≤ lat (CK-42) ≤ " + roundDouble(CoordTranslator.toCK42Lat(theParser.getMaxLat(), theParser.getMaxLon(), theParser.getAltFromLatLon(theParser.getMaxLat(), theParser.getMaxLon()))) + "\n";
                                    successOutput += roundDouble(CoordTranslator.toCK42Lon(theParser.getMinLat(), theParser.getMinLon(), theParser.getAltFromLatLon(theParser.getMinLat(), theParser.getMinLon()))) + " ≤ lon (CK-42) ≤ " + roundDouble(CoordTranslator.toCK42Lon(theParser.getMaxLat(), theParser.getMaxLon(), theParser.getAltFromLatLon(theParser.getMaxLat(), theParser.getMaxLon()))) + "\n\n";
                                } catch (RequestedValueOOBException e) { // This shouldn't happen, may be possible though if GeoTIFF file is very small
                                    // revert to WGS84 if CK-42 conversion has failed
                                    successOutput += getString(R.string.wgs84_ck42_conversion_fail_warning);
                                    successOutput += roundDouble(theParser.getMinLat()) + " ≤ lat ≤ " + roundDouble(theParser.getMaxLat()) + "\n";
                                    successOutput += roundDouble(theParser.getMinLon()) + " ≤ lon ≤ " + roundDouble(theParser.getMaxLon()) + "\n\n";
                                }
                            }
                            appendText(successOutput);
                            isDEMLoaded = true;
                            setButtonReady(buttonSelectImage, true);
                            if (isImageLoaded) {
                                setButtonReady(buttonCalculate, true);
                            }
                            progressBar.setVisibility(View.GONE);
                        } else {
                            appendText(e.getMessage());
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }).start();
    }

    private Exception loadDEMnewThread(Uri uri) {
        File appCacheDir = new File(getCacheDir(), "geotiff");
        if (!appCacheDir.exists()) {
            appCacheDir.mkdirs();
        }
        // Android 10/11, we can't access this file directly
        // We will copy the file into app's own package cache
        File fileInCache = new File(appCacheDir, uri.getLastPathSegment());
        if (!isCacheUri(uri)) {
            try {
                try (InputStream inputStream = getContentResolver().openInputStream(uri);
                     OutputStream outputStream = new FileOutputStream(fileInCache)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (FileNotFoundException e) {
                    // Handle the FileNotFoundException here
                    // For example, you can show an error message to the user
                    // or log the error to Crashlytics
                    Log.e(TAG, "FileNotFound demSelected()");
                    throw e;
                } catch (IOException e) {
                    // Handle other IOException here
                    // For example, you can log the error to Crashlytics
                    e.printStackTrace();
                    throw e;
                } finally {
                    setButtonReady(buttonSelectDEM, true);
                }
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
                return e;
            }
        }
        demUri = Uri.fromFile(fileInCache);

        try {
            GeoTIFFParser parser = new GeoTIFFParser(fileInCache);
            theParser = parser;
            theTGetter = new TargetGetter(parser);
            return null;
        } catch (IllegalArgumentException e) {
            String failureOutput = getString(R.string.dem_load_error_generic_msg);
            e.printStackTrace();
            return new Exception(failureOutput + "\n");
        } catch (TiffException e) {
            String failureOutput = getString(R.string.dem_load_error_tiffexception_msg);
            e.printStackTrace();
            return new Exception(failureOutput + "\n");
        } finally {
            setButtonReady(buttonSelectDEM, true);
        }
    }

    private boolean isCacheUri(Uri uri) {

        File cacheDir = getCacheDir();
        String cachePath = cacheDir.getAbsolutePath();
        String uriPath = uri.getPath();
        return uriPath.startsWith(cachePath);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;

        int id = item.getItemId();

        // don't do anything if user chooses Calculate; we're already there.

        if (id == R.id.action_prefs) {
            intent = new Intent(getApplicationContext(), PrefsActivity.class);
            // https://stackoverflow.com/questions/8688099/android-switch-to-activity-without-restarting-it
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_about) {
            intent = new Intent(getApplicationContext(),AboutActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_log) {
            intent = new Intent(getApplicationContext(),ActivityLog.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume started");
        super.onResume();
        if (!isTargetCoordDisplayed) {
            restorePrefOutputMode(); // reset the textViewTargetCoord display
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Permissions Granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Failed to Obtain Necessary Permissions", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onPause()
    {
        Log.d(TAG,"onPause started");
        //appendText("onPause\n");
        super.onPause();

    } // onPause()

    @Override
    protected void onDestroy()
    {
        Log.d(TAG,"onDestroy started");
        //appendText("onDestroy\n");
        // do whatever here

        // no need to close logfile; its closed after each use

        super.onDestroy();

    } // onDestroy()

    // http://android-er.blogspot.com/2009/12/read-exif-information-in-jpeg-file.html
    private String getTagString(String tag, ExifInterface exif)
    {
        return(tag + " : " + exif.getAttribute(tag) + "\n");
    }

    public void calculateImage(View view)
    {
        Drawable aDrawable;
        ExifInterface exif;
        String attribs = "Exif information ---\n";

        clearText();
        textViewTargetCoord.setText("");

        appendText(getString(R.string.calculating_target_msg));
        appendLog("Going to start calculation\n");

        if (imageUri == null) {
            appendLog("ERROR: Cannot calculate \uD83D\uDEAB\uD83E\uDDEE; no image \uD83D\uDEAB\uD83D\uDDBC selected\n");
            appendText(getString(R.string.no_image_selected_error_msg));
            return;
        }

        // load image into object
        try {
            ContentResolver cr = getContentResolver();
            InputStream is = cr.openInputStream(imageUri);
            aDrawable = iView.getDrawable();
            exif = new ExifInterface(is);

            double[] values = getMetadataValues(exif);
            double y = values[0];
            double x  = values[1];
            double z = values[2];
            double azimuth = values[3];
            double theta = values[4];

            Log.i(TAG, "parsed xmpMeta\n");

            appendText(getString(R.string.opened_exif_for_image_msg));
            attribs += getTagString(ExifInterface.TAG_DATETIME, exif);
            attribs += getTagString(ExifInterface.TAG_MAKE, exif);
            attribs += getTagString(ExifInterface.TAG_MODEL, exif);

            if (!outputModeIsSlavic()) {
                attribs += "Latitude : " + roundDouble(y) + "\n";
                attribs += "Longitude : " + roundDouble(x) + "\n";
                attribs += "Altitude : " + Math.round(z) + "\n";
            } else {
                attribs += "Latitude (WGS84): " + roundDouble(y) + "\n";
                attribs += "Longitude (WGS84): " + roundDouble(x) + "\n";
                attribs += "Altitude (WGS84): " + Math.round(z) + "\n";
            }

            attribs += getString(R.string.attribute_text_drone_azimuth) + " " + roundDouble(azimuth) + "\n";
            attribs += getString(R.string.attribute_text_drone_camera_pitch) + " -" + roundDouble(theta) + "\n";
            appendText(attribs);
            attribs = "";
            double[] result;
            double distance;
            double latitude;
            double longitude;
            long altitude;

            double latCK42;
            double lonCK42;
            long altCK42;

            long GK_zone;
            long GK_northing;
            long GK_easting;
            if (theTGetter != null) {
                try {
                    result = theTGetter.resolveTarget(y, x, z, azimuth, theta);
                    distance = result[0];
                    latitude = result[1];
                    longitude = result[2];
                    double altitudeDouble = result[3];
                    latCK42 = CoordTranslator.toCK42Lat(latitude, longitude, altitudeDouble);
                    lonCK42 = CoordTranslator.toCK42Lon(latitude, longitude, altitudeDouble);
                    // Note: This altitude calculation assumes the SK42 and WGS84 ellipsoid have the exact same center
                    //     This is not totally correct, but in practice is close enough to the actual value
                    //     @TODO Could be refined at a later time with better math
                    //     See: https://gis.stackexchange.com/a/88499
                    altCK42 = Math.round(altitudeDouble - CoordTranslator.fromCK42Alt(latCK42, lonCK42, 0.0d));

                    long[] GK_conversion_results = CoordTranslator.fromCK42toCK42_GK(latCK42, lonCK42);
                    GK_zone = GK_conversion_results[0];
                    GK_northing = GK_conversion_results[1];
                    GK_easting = GK_conversion_results[2];

                    altitude = Math.round(result[3]);
                    if (!outputModeIsSlavic()) {
                        attribs += getString(R.string.target_found_at_msg) + ": " + roundDouble(latitude) + "," + roundDouble(longitude) + " Alt: " + altitude + "m" + "\n";
                    } else {
                        attribs += getString(R.string.target_found_at_msg) + " (WGS84): " + roundDouble(latitude) + "," + roundDouble(longitude) + " Alt: " + altitude + "m" + "\n";
                        attribs += getString(R.string.target_found_at_msg) + " (CK-42): " + roundDouble(latCK42) + "," + roundDouble(lonCK42) + " Alt: " + altCK42 + "m" + "\n";
                    }
                    attribs += getString(R.string.drone_dist_to_target_msg) + " " + Math.round(distance) + "m\n";
                    if (!outputModeIsSlavic()) { // to avoid confusion with WGS84, no Google Maps link is provided when outputModeIsSlavic()
                        attribs += "<a href=\"https://maps.google.com/?q=" + roundDouble(latitude) + "," + roundDouble(longitude) + "\">";
                        attribs += "maps.google.com/?q=" + roundDouble(latitude) + "," + roundDouble(longitude) + "</a>\n";
                    }
                } catch (RequestedValueOOBException e) {
                    if (e.isAltitudeDataBad) {
                        Log.e(TAG, e.getMessage());
                        attribs += getString(R.string.bad_altitude_data_error_msg) + "\n";
                        appendText(attribs);
                        return;
                    } else {
                        Log.e(TAG, "ERROR: resolveTarget ran OOB at (WGS84): " + roundDouble(e.OOBLat) + ", " + roundDouble(e.OOBLon));
                        if (!outputModeIsSlavic()) {
                            attribs += getString(R.string.resolveTarget_oob_error_msg) + ":" + roundDouble(e.OOBLat) + ", " + roundDouble(e.OOBLon) + "\n";
                        } else {
                            attribs += getString(R.string.resolveTarget_oob_error_msg) + " (CK-42):" + roundDouble(CoordTranslator.toCK42Lat(e.OOBLat, e.OOBLon, z)) + ", " + roundDouble(CoordTranslator.toCK42Lon(e.OOBLat, e.OOBLon, z)) + "\n";
                        }
                        attribs += getString(R.string.geotiff_coverage_reminder);
                        attribs += getString(R.string.geotiff_coverage_precedent_message);
                        if (!outputModeIsSlavic()) {
                            attribs += roundDouble(theParser.getMinLat()) + " ≤ lat ≤ " + roundDouble(theParser.getMaxLat()) + "\n";
                            attribs += roundDouble(theParser.getMinLon()) + " ≤ lon ≤ " + roundDouble(theParser.getMaxLon()) + "\n\n";
                        } else {
                            try {
                                // Believe me, I don't like this either....
                                attribs += roundDouble(CoordTranslator.toCK42Lat(theParser.getMinLat(), theParser.getMinLon(), theParser.getAltFromLatLon(theParser.getMinLat(), theParser.getMinLon()))) + " ≤ lat (CK-42) ≤ " + roundDouble(CoordTranslator.toCK42Lat(theParser.getMaxLat(), theParser.getMaxLon(), theParser.getAltFromLatLon(theParser.getMaxLat(), theParser.getMaxLon()))) + "\n";
                                attribs += roundDouble(CoordTranslator.toCK42Lon(theParser.getMinLat(), theParser.getMinLon(), theParser.getAltFromLatLon(theParser.getMinLat(), theParser.getMinLon()))) + " ≤ lon (CK-42) ≤ " + roundDouble(CoordTranslator.toCK42Lon(theParser.getMaxLat(), theParser.getMaxLon(), theParser.getAltFromLatLon(theParser.getMaxLat(), theParser.getMaxLon()))) + "\n";
                            } catch (RequestedValueOOBException e_OOB) { // This shouldn't happen, may be possible though if GeoTIFF file is very small
                                // revert to WGS84 if CK-42 conversion has failed
                                attribs += getString(R.string.wgs84_ck42_conversion_fail_warning);
                                attribs += roundDouble(theParser.getMinLat()) + " ≤ lat ≤ " + roundDouble(theParser.getMaxLat()) + "\n";
                                attribs += roundDouble(theParser.getMinLon()) + " ≤ lon ≤ " + roundDouble(theParser.getMaxLon()) + "\n\n";
                            }                        }
                        appendText(attribs);
                        return;
                    }
                }
            } else {
                attribs += getString(R.string.geotiff_load_reminder_msg);
                appendText(attribs);
                return;
            }
            attribs = attribs.replaceAll("(\r\n|\n)", "<br>"); // replace newline with HTML equivalent
            textView.append(Html.fromHtml(attribs, 0, null, null));
            // Obtain NATO MGRS from mil.nga.mgrs library
            MGRS mgrsObj = MGRS.from(new Point(longitude, latitude));
            String mgrs1m = mgrsObj.coordinate(GridType.METER);
            String mgrs10m = mgrsObj.coordinate(GridType.TEN_METER);
            String mgrs100m = mgrsObj.coordinate(GridType.HUNDRED_METER);
            String targetCoordString;
            if (!outputModeIsSlavic()) {
                targetCoordString = "<a href=\"https://maps.google.com/?q=";
                if (outputModeIsMGRS()) {
                    targetCoordString += mgrs1m; // use MGRS 1m for maps link, even if on 10m or 100m mode
                } else {
                    targetCoordString += roundDouble(latitude) + "," + roundDouble(longitude); // otherwise just use normal WGS84
                }
                targetCoordString += "\">"; // close start of href tag
                if (outputModeIsMGRS()) {
                    switch(outputMode) {
                        case MGRS1m:
                            targetCoordString += mgrs1m;
                            break;
                        case MGRS10m:
                            targetCoordString += mgrs10m;
                            break;
                        case MGRS100m:
                            targetCoordString += mgrs100m;
                            break;
                        default:
                            throw new RuntimeException("Program entered an inoperable state due to outputMode"); // this shouldn't ever happen
                    }
                } else {
                    targetCoordString += roundDouble(latitude) + ", " + roundDouble(longitude);
                }
                targetCoordString += "</a> "; // end href tag
                targetCoordString += "Alt: " + altitude + "m";
            } else { // to avoid confusion with WGS84, no Google Maps link is provided when outputModeIsSlavic()
                if (outputMode == outputModes.CK42Geodetic) {
                    targetCoordString = "(CK-42) " + roundDouble(latCK42) + ", " + roundDouble(lonCK42) + " Alt: " + altCK42 + "m" + "<br>";
                } else if (outputMode == outputModes.CK42GaussKrüger) {
                    targetCoordString = "(CK-42) [Gauss-Krüger] " + getString(R.string.gk_zone_text) + " " + GK_zone + "<br>" + getString(R.string.gk_northing_text) + " " + GK_northing + "<br>" + getString(R.string.gk_easting_text) + " " + GK_easting + "<br>" + "Alt:" + " " + altCK42 + "m";
                } else {
                    throw new RuntimeException("Program entered an inoperable state due to outputMode"); // this shouldn't ever happen
                }
            }
            textViewTargetCoord.setText(Html.fromHtml(targetCoordString, 0, null, null));
            isTargetCoordDisplayed = true;
            // close file
            is.close();
            //
        } catch (XMPException e) {
            Log.e(TAG, e.getMessage());
            appendText(getString(R.string.metadata_parse_error_msg) + e + "\n");
            e.printStackTrace();
        } catch (MissingDataException e) {
            Log.e(TAG, e.getMessage());
            appendText(e.getMessage() + "\n");
            e.getStackTrace();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            appendText(getString(R.string.metadata_parse_error_msg)+e+"\n");
            e.printStackTrace();
        }
    } // button click

    private double[] getMetadataValues(ExifInterface exif) throws XMPException, MissingDataException {
        if (exif == null) {
            Log.e(TAG, "ERROR: getMetadataValues failed, ExifInterface was null");
            throw new IllegalArgumentException("ERROR: getMetadataValues failed, exif was null");
        }
        String make = exif.getAttribute(ExifInterface.TAG_MAKE);
        String model = exif.getAttribute(ExifInterface.TAG_MODEL);
        if (make == null || make.equals("")) {
            return null;
        }
        make = make.toUpperCase();
        model = model.toUpperCase();
        switch(make) {
            case "DJI":
                return handleDJI(exif);
                //break;
            case "SKYDIO":
                return handleSKYDIO(exif);
                //break;
            case "AUTEL ROBOTICS":
                if (dangerousAutelAwarenessCount < 3) {
                    displayAutelAlert();
                }
                return handleAUTEL(exif);
                //break;
            case "PARROT":
                if (model.contains("ANAFI")) {
                    return handlePARROT(exif);
                } else {
                    Log.e(TAG, "ERROR: Parrot model " + model + " not usable at this time");
                    throw new XMPException("ERROR: Parrot model " + model + " not usable at this time", XMPError.BADVALUE);
                }
                //break;
            default:
                Log.e(TAG, "ERROR: make " + make + " not usable at this time");
                throw new XMPException("ERROR: make " + make + " not usable at this time", XMPError.BADXMP);
        }
    }

    private double[] handleDJI(ExifInterface exif) throws XMPException, MissingDataException{
        String xmp_str = exif.getAttribute(ExifInterface.TAG_XMP);
        if (xmp_str == null) {
            throw new XMPException("ERROR: XMP tag not found within EXIF", XMPError.BADXMP);
        } if (xmp_str.trim().equals("")) {
            throw new XMPException("ERROR: XMP tag found but was empty!", XMPError.BADVALUE);
        }
        Log.i(TAG, "xmp_str for Make DJI: " + xmp_str);
        XMPMeta xmpMeta = XMPMetaFactory.parseFromString(xmp_str.trim());

        String schemaNS = "http://www.dji.com/drone-dji/1.0/";
        String latitude = xmpMeta.getPropertyString(schemaNS, "GpsLatitude");
        double y;
        if (latitude != null) {
            y = Double.parseDouble(latitude);
        } else {
            throw new MissingDataException(getString(R.string.missing_data_exception_latitude_error_msg), MissingDataException.dataSources.EXIF_XMP, MissingDataException.missingValues.LATITUDE);
        }
        String longitude = xmpMeta.getPropertyString(schemaNS, "GpsLongitude");
        if (longitude == null || longitude.equals("")) {
            // handle a typo "GpsLongtitude" that occurs in certain versions of Autel drone firmware (which use drone-dji metadata format)
            longitude = xmpMeta.getPropertyString(schemaNS, "GpsLong" + "t" + "itude");
            if (longitude == null || longitude.equals("")) {
                throw new MissingDataException(getString(R.string.missing_data_exception_longitude_error_msg), MissingDataException.dataSources.EXIF_XMP, MissingDataException.missingValues.LATITUDE);
            }
        }
        double x = Double.parseDouble(longitude);

        double z;
        String altitude = xmpMeta.getPropertyString(schemaNS, "AbsoluteAltitude");
        if (altitude != null) {
            z = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "AbsoluteAltitude"));
        } else {
            throw new MissingDataException(getString(R.string.missing_data_exception_altitude_error_msg), MissingDataException.dataSources.EXIF_XMP, MissingDataException.missingValues.ALTITUDE);
        }

        double azimuth;
        String gimbalYawDegree = xmpMeta.getPropertyString(schemaNS, "GimbalYawDegree");
        if (gimbalYawDegree != null) {
            azimuth = Double.parseDouble(gimbalYawDegree);
        } else {
            throw new MissingDataException(getString(R.string.missing_data_exception_azimuth_error_msg), MissingDataException.dataSources.EXIF_XMP, MissingDataException.missingValues.AZIMUTH);
        }

        double theta;
        String gimbalPitchDegree = xmpMeta.getPropertyString(schemaNS, "GimbalPitchDegree");
        if (gimbalPitchDegree != null) {
            theta = Math.abs(Double.parseDouble(gimbalPitchDegree));
        } else {
            throw new MissingDataException(getString(R.string.missing_data_exception_theta_error_msg), MissingDataException.dataSources.EXIF_XMP, MissingDataException.missingValues.THETA);
        }

        // safety check: if metadata azimuth and theta are zero, it's extremely likely the metadata is invalid
        if (Math.abs(Double.compare(azimuth, 0.0d)) <= 0.001d && Math.abs(Double.compare(theta, 0.0d)) <= 0.001d) {
            throw new MissingDataException(getString(R.string.missing_data_exception_altitude_and_theta_error_msg), MissingDataException.dataSources.EXIF_XMP, MissingDataException.missingValues.THETA);
        }

        double[] outArr = new double[]{y, x, z, azimuth, theta};
        return outArr;
    }

    private double[] handleSKYDIO(ExifInterface exif) throws XMPException {
        String xmp_str = exif.getAttribute(ExifInterface.TAG_XMP);
        if (xmp_str == null) {
            throw new XMPException("ERROR: XMP tag not found within EXIF", XMPError.BADXMP);
        } if (xmp_str.trim().equals("")) {
            throw new XMPException("ERROR: XMP tag found but was empty!", XMPError.BADVALUE);
        }
        Log.i(TAG, "xmp_str for Make SKYDIO: " + xmp_str);
        XMPMeta xmpMeta = XMPMetaFactory.parseFromString(xmp_str.trim());
        String schemaNS = "https://www.skydio.com/drone-skydio/1.0/";
        double y = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "Latitude"));
        double x = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "Longitude"));
        double z = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "AbsoluteAltitude"));

        double azimuth = Double.parseDouble(xmpMeta.getStructField(schemaNS, "CameraOrientationNED", schemaNS, "Yaw").getValue());
        double theta = Double.parseDouble(xmpMeta.getStructField(schemaNS, "CameraOrientationNED", schemaNS, "Pitch").getValue());
        theta = Math.abs(theta);

        double[] outArr = new double[]{y, x, z, azimuth, theta};
        return outArr;
    }

    private double[] handleAUTEL(ExifInterface exif) throws XMPException, MissingDataException{
        String xmp_str = exif.getAttribute(ExifInterface.TAG_XMP);
        if (xmp_str == null) {
            throw new XMPException("ERROR: XMP tag not found within EXIF", XMPError.BADXMP);
        } if (xmp_str.trim().equals("")) {
            throw new XMPException("ERROR: XMP tag found but was empty!", XMPError.BADVALUE);
        }
        Log.i(TAG, "xmp_str for Make AUTEL: " + xmp_str);
        XMPMeta xmpMeta = XMPMetaFactory.parseFromString(xmp_str.trim());

        boolean isNewMetadataFormat;
        int aboutIndex = xmp_str.indexOf("rdf:about=");
        String rdf_about = xmp_str.substring(aboutIndex + 10, aboutIndex + 24); // not perfect, should be fine though
        Log.d(TAG, "rdf_about: " + rdf_about);

        if (!rdf_about.toLowerCase().contains("autel")) {
            isNewMetadataFormat = true;
        } else {
            isNewMetadataFormat = false;
        }

        double y;
        double x;
        double z;
        double azimuth;
        double theta;

        if (isNewMetadataFormat) {
            // Newer metadata uses the same format and schemaNS as DJI
            return handleDJI(exif);
        } else {
            Float[] yxz = exifGetYXZ(exif);
            y = yxz[0];
            x = yxz[1];
            z = yxz[2];

            String schemaNS = "http://pix4d.com/camera/1.0";

            azimuth = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "Yaw"));
            theta = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "Pitch"));
            // AUTEL old firmware Camera pitch 0 is down, 90 is forwards towards horizon
            // so, we use the complement of the angle instead
            // see: https://support.pix4d.com/hc/en-us/articles/202558969-Yaw-Pitch-Roll-and-Omega-Phi-Kappa-angles
            theta = 90.0d - theta;
            double[] outArr = new double[]{y, x, z, azimuth, theta};
            return outArr;
        }
    }

    private double[] handlePARROT(ExifInterface exif) throws XMPException {
        double y;
        double x;
        double z;

        Float[] yxz = exifGetYXZ(exif);
        y = yxz[0];
        x = yxz[1];
        z = yxz[2];

        String xmp_str = exif.getAttribute(ExifInterface.TAG_XMP);
        if (xmp_str == null) {
            throw new XMPException("ERROR: XMP tag not found within EXIF", XMPError.BADXMP);
        } if (xmp_str.trim().equals("")) {
            throw new XMPException("ERROR: XMP tag found but was empty!", XMPError.BADVALUE);
        }
        Log.i(TAG, "xmp_str for Make PARROT: " + xmp_str);
        XMPMeta xmpMeta = XMPMetaFactory.parseFromString(xmp_str.trim());

        String schemaNS = "http://www.parrot.com/drone-parrot/1.0/";

        double azimuth = Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "CameraYawDegree"));
        double theta = Math.abs(Double.parseDouble(xmpMeta.getPropertyString(schemaNS, "CameraPitchDegree")));
        double[] outArr = new double[]{y, x, z, azimuth, theta};
        return outArr;
    }

    private void displayAutelAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.autel_accuracy_warning_msg);
        builder.setPositiveButton(R.string.i_understand_this_risk, (DialogInterface.OnClickListener) (dialog, which) -> {
            dangerousAutelAwarenessCount += 1;
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void copyTargetCoordText(View view) {
        if (isTargetCoordDisplayed) {
            String text = textViewTargetCoord.getText().toString();
            text = text.replaceAll("<[^>]*>", ""); // remove HTML link tag(s)

            // Copy the text to the clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Text", text);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, getString(R.string.text_copied_to_clipboard_msg), Toast.LENGTH_SHORT).show();
        }
    }

    // select image button clicked; launch chooser and get result
    // in callback
    public void selectImage(View view)
    {
        Log.d(TAG,"selectImageClick started");
        Log.d(TAG,"READ_EXTERNAL_STORAGE: " + Integer.toString(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)));

        requestExternStorage();

        appendLog("Going to start selecting image\n");
        //appendText("selectImageClick started\n");

        //Intent i = new Intent();
        //i.setType("image/*");
        //i.setAction(Intent.ACTION_GET_CONTENT);
        //startActivity(Intent.createChooser(i,"Select Picture"));

        mGetContent.launch("image/*");

        // pass the constant to compare it
        // with the returned requestCode
        // StartActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);

        //appendText("Chooser started\n");
        appendLog("Chooser started\n");
    }

    public void selectDEM(View view)
    {
        Log.d(TAG,"selectDEM started");
        appendLog("Going to start selecting GeoTIFF\n");

        Log.d(TAG,"READ_EXTERNAL_STORAGE: " + Integer.toString(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)));

        requestExternStorage();

        mGetDEM.launch("image/*");

    }

    private void requestExternStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                Log.d(TAG, "Attempting to Obtain unobtained storage permissions");
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, requestNo);
                requestNo++;
            }
        }
    }

    private Float[] exifGetYXZ(ExifInterface exif)
    {
        String latDir = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        latDir = latDir.toUpperCase();
        String latRaw = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String[] latArr = latRaw.split(",", 3);
        String lonDir = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        lonDir = lonDir.toUpperCase();
        String lonRaw = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String[] lonArr = lonRaw.split(",", 3);
        String alt = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);

        float y = 0.0f;
        y += rationalToFloat(latArr[0]);
        y += rationalToFloat(latArr[1]) / 60.0f;
        y += rationalToFloat(latArr[2]) / 3600.0f;
        if (latDir.equals("S"))
        {
            y = y * -1.0f;
        }

        float x = 0.0f;
        x += rationalToFloat(lonArr[0]);
        x += rationalToFloat(lonArr[1]) / 60.0f;
        x += rationalToFloat(lonArr[2]) / 3600.0f;
        if (lonDir.equals("W"))
        {
            x = x * -1.0f;
        }

        float z = rationalToFloat(alt);

        Float[] arrOut = {y, x, z};
        return(arrOut);
    }

    private float rationalToFloat(String str)
    {
        String[] split = str.split("/", 2);
        float numerator = Float.parseFloat(split[0]);
        float denominator = Float.parseFloat(split[1]);
        return numerator / denominator;
    }

    private String roundDouble(double d) {
        DecimalFormat df = new DecimalFormat("#.######");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(d);
    }

    private void appendText(final String aStr)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(aStr);

            }
        });

    } // appendText to textView but do so on UI thread

    // reset the text field
    private void clearText()
    {
        runOnUiThread(new Runnable() {
           @Override
           public void run() {
               String placeholderText = "OpenAthena™ for Android version "+versionName+"\n\n";
               placeholderText += "Step 1: load a Digital Elevation Model (DEM) \u26F0\n";
               placeholderText += "Step 2: load a Drone Image \uD83D\uDDBC\n";
               placeholderText += "Step 3: press the \uD83E\uDDEE button to calculate\n";
               placeholderText += "Step 4: obtain your target location below \uD83C\uDFAF\n\n";
               textView.setText(placeholderText);
           }
        });

    }

    private void appendLog(String str)
    {
        FileOutputStream fos;
        PrintWriter pw;

        Log.d(TAG,"appendLog started");

        try {
            fos = openFileOutput(MainActivity.LOG_NAME, Context.MODE_PRIVATE|Context.MODE_APPEND);
            pw = new PrintWriter(fos);
            pw.print(str);
            pw.close();
            fos.close();
            Log.d(TAG,"appendLog: wrote to logfile");

        } catch (Exception e) {
            Log.d(TAG,"appendLog: failed to write log:"+e.getMessage());
        }

    } // appendLog()


}
