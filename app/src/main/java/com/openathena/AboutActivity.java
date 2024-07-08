// AboutActivity.java
// OpenAthena for Android
// Bobby Krupczak, Matthew Krupczak, et. al

package com.openathena;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.PrintWriter;

public class AboutActivity extends AppCompatActivity {

    public static String TAG = AboutActivity.class.getSimpleName();
    TextView aboutText;
    String versionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG,"onCreate started");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        aboutText = (TextView)findViewById(R.id.aboutText);
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());

        // try to get our version out of app/build.gradle
        // versionName field
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
            Log.d(TAG, "Got version " + versionName);
        }
        catch (Exception e) {
            versionName = "unknown";
        }

        // set About text
        aboutText.setText(Html.fromHtml( getString(R.string.app_name) + " " + getString(R.string.version_word) + " " + versionName+"<br>"
                        + "© 2024 Theta Informatics LLC<br>"
                        + getString(R.string.AGPLv3Notice)
                        + "<br> <a href=\"https://openathena.com/\">OpenAthena.com</a> <br>"
                        + getString(R.string.AboutSnippet) + "<br>"
                        + getString(R.string.CalibrationReminderSnippet) + "<br>"
                        + "<br><br><a href=\"https://github.com/mkrupczak3/OpenAthenaAndroid\">"
                        + getString(R.string.CallToActionSnippet)
                        + "<br>" + getString(R.string.nato_vertical_datum_notice) + "<br>"
                        + getString(R.string.warsaw_vertical_datum_notice) + "<br>"
                        + getString(R.string.AuthorGitHubSnippet)
                        + getString(R.string.about_software_libraries_used) + "<br>"
                        + "<a href=\"https://github.com/ngageoint/tiff-java\">ngageoint/tiff-java</a> MIT license<br>"
                        + "<a href=\"https://github.com/ngageoint/mgrs-android/\">ngageoint/mgrs-android</a> MIT license<br>"
                        + "<a href=\"https://github.com/veraPDF/veraPDF-xmp\">veraPDF/veraPDF-xmp</a> Adobe Community license<br>"
                        + "<a href=\"https://github.com/matthiaszimmermann/EGM96\">matthiaszimmermann/EGM96</a> MIT license<br>"
                        + "<a href=\"https://github.com/agilesrc/dem4j\">agilesrc/dem4j</a> Apache-2.0 license<br>"
                        + "<a href=\"https://github.com/ThreeTen/threetenbp\">ThreeTen/threetenbp</a> BSD 3-Clause<br>"
                        + "<a href=\"https://github.com/apache/commons-lang\">Apache Commons Lang</a> Apache License<br>"
                        + "<a href=\"https://github.com/apache/commons-io\">Apache Commons IO</a> Apache License<br><br>"
                        + "This project uses the OpenTopography.org website API for DEM downloading within the app. OpenTopography.org's privacy policy is available here: <a href=\"https://opentopography.org/privacypolicy\">https://opentopography.org/privacypolicy</a><br>"
                        + "OpenTopography is operated by the University of California San Diego with support from the National Science Foundation. It is not affiliated with Theta Informatics LLC or the OpenAthena project."
                ,0,null, null)
        );

    } // onCreate()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;

        int id = item.getItemId();

        if (id == R.id.action_managedems) {
            // jump to manage dem cache activity
            intent = new Intent(getApplicationContext(),ManageDemsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }
        
        if (id == R.id.action_calculate) {
            // jump to main activity
            // its already created
            intent = new Intent(getApplicationContext(),MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_prefs) {
            intent = new Intent(getApplicationContext(), PrefsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }

//        if (id == R.id.action_log) {
//            intent = new Intent(getApplicationContext(),ActivityLog.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            startActivity(intent);
//            return true;
//        }

        // don't do anything if about is selected as we are already there

        return super.onOptionsItemSelected(item);
    }

        @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG,"onPause");

        //appendText("onPause\n");
        super.onPause();

    } // onPause()

    @Override
    protected void onDestroy()
    {
        Log.d(TAG,"onDestroy started");

        // close logfile
        //appendText("onDestroy\n");
        // do whatever here
        super.onDestroy();

    } // onDestroy()

    private void appendText(final String aStr)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aboutText.append(aStr);
            }
        });

    } // appendText to textView but do so on UI thread

//    private void appendLog(String str)
//    {
//        FileOutputStream fos;
//        PrintWriter pw;
//
//        Log.d(TAG,"appendLogLocal started");
//
//        try {
//            fos = openFileOutput(MainActivity.LOG_NAME, Context.MODE_PRIVATE|Context.MODE_APPEND);
//            pw = new PrintWriter(fos);
//            pw.print(str);
//            pw.close();
//            fos.close();
//            Log.d(TAG,"appendLogLocal: wrote to logfile");
//
//        } catch (Exception e) {
//            Log.d(TAG,"appendLogLocal: failed to write log:"+e.getMessage());
//        }
//
//    } // appendLog()
}
