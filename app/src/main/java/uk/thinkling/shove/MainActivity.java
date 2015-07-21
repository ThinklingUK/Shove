package uk.thinkling.shove;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;


public class MainActivity extends Activity {


    ShoveDrawView myDrawView;
    RelativeLayout overlay, instructions;
    Handler mHandler;
    SoundPool player;
    int clinkSound, clunkSound, placeSound, slideSound;

    public TextView HighScoreText, InstructionText;
    ViewGroup parent;
    int index;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // find the text views
        HighScoreText = (TextView) findViewById(R.id.HighScoreText);
        InstructionText = (TextView) findViewById(R.id.textInstructions);

        // find the drawView, parent and index (for switching)
        myDrawView = (ShoveDrawView) findViewById(R.id.drawView);
        overlay = (RelativeLayout) findViewById(R.id.overlay);
        instructions = (RelativeLayout) findViewById(R.id.instructions);
        parent = (ViewGroup) myDrawView.getParent();
        index = parent.indexOfChild(myDrawView);

        // start the timer handler that will invalidate the view
        mHandler = new Handler();

        //set up the sound player

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createSoundPoolWithBuilder();
        } else{
            createSoundPoolWithConstructor();
        }
        clinkSound = player.load(this, R.raw.clink, 1);
        clunkSound = player.load(this, R.raw.clunk, 1);
        placeSound = player.load(this, R.raw.plonk, 1);
        slideSound = player.load(this, R.raw.slide, 1);
    }


    @Override
    protected void onPause() {
        super.onPause();

        // Save application preferences data - settings should also be stored here
/*        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("hiscore", 99);
        editor.putString("test", "preferences OK");
        editor.commit();*/
        //stop the callback loop
        mHandler.removeCallbacks(mRunnable);

        try {
            myDrawView.saveData();
            //Toast.makeText(getBaseContext(), "onPause - OK", Toast.LENGTH_SHORT).show();
        } catch (IOException ex) {
            //Toast.makeText(getBaseContext(), "onPause - Fail", Toast.LENGTH_SHORT).show();
            //Log.e("onPause", ex.toString());
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();


            try {
                myDrawView.loadPrefs();
                InstructionText.setText(myDrawView.dynamicInstructions);
                //Toast.makeText(getBaseContext(), "onResume - OK", Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                //Toast.makeText(getBaseContext(), "onResume - Fail", Toast.LENGTH_SHORT).show();
            }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //all of this is in the getsizechange of drawView
/*        Toast.makeText(getBaseContext(), "onStart", Toast.LENGTH_SHORT).show();
        String test ="";
        try {
            //Load lists from file or set defaults for some reason, | is not good delimiter
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int hiscore = preferences.getInt("hiscore", 0);
            test = preferences.getString("test", "prefs not found");

        } catch (Exception e){
            Log.d("LOADING LIST",  e.getMessage());
        }
        Toast.makeText(this, test, Toast.LENGTH_SHORT).show();*/


    }

    // Sound pool builder - TODO should converge with constructor version
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createSoundPoolWithBuilder(){
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        player = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(6).build();
    }

    @SuppressWarnings("deprecation")
    protected void createSoundPoolWithConstructor(){
        player = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
    }

    // this is the runnable action that is called by the handler - it will add itself to the handler again after delay
    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {

            // Update the coin positions and scoring etc.
            myDrawView.update();
            /** invalidate the view - forcing it to redraw **/
            myDrawView.invalidate();

            //do this again in 40 milliseconds - ie. 1/25th of a second
            mHandler.postDelayed(mRunnable, 40);
        }
    };


    @Override
    // Create the top bar menu options
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

/*
    @Override
    //handle the clicks on the top bar menu options
        public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()){

            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent); //TODO should really startActivitywithResult and capture here ...
                return true;

            case R.id.action_clearcache:
                File file = new File(getCacheDir(), "moveObjs");
                if (file.exists()) file.delete();
                file = new File(getCacheDir(), "Scores");
                if (file.exists()) file.delete();
                return true;

            case R.id.action_start:
                parent.removeView(myDrawView);
                myDrawView = new ShoveDrawView(this , null);
                break;

            default:

        }

        parent.addView(myDrawView, index);
        return super.onOptionsItemSelected(item);
    }
*/

    // method to attach to button onclick (via listener or XML)
    public void onPressButton(View v) {

        switch (v.getId()){

            case R.id.button_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent); //TODO should really startActivitywithResult and capture here ...
                break;

            case R.id.button_instructions:
                InstructionText.setText(Html.fromHtml(myDrawView.dynamicInstructions));
                overlay.setVisibility(View.GONE);
                instructions.setVisibility(View.VISIBLE);
                break;


            case R.id.button_close_instructions:
                overlay.setVisibility(View.VISIBLE);
                instructions.setVisibility(View.GONE);
                break;


            //TODO id changes - display if game in play
            case R.id.button_newgame:

                //clear cache
                File file = new File(getCacheDir(), "moveObjs");
                if (file.exists()) file.delete();
                file = new File(getCacheDir(), "Scores");
                if (file.exists()) file.delete();

                // totally new game
                parent.removeView(myDrawView);
                myDrawView = new ShoveDrawView(this , null);
                parent.addView(myDrawView, index);



                // drop through to resume / play

            case R.id.button_resume:

            case R.id.playButton:
                overlay.setVisibility(View.GONE);
                // start the timer handler that will process moves & invalidate the view
                mHandler.post(mRunnable);
                break;

            case R.id.pauseButton:
                overlay.setVisibility(View.VISIBLE);
                mHandler.removeCallbacks(mRunnable); //stop the callback loop
                break;
            default:

        }
    }

    @Override
    public void onBackPressed() {
        if (instructions.getVisibility()==View.VISIBLE ) { //NB: close instructions if shown
            instructions.setVisibility(View.GONE);
            overlay.setVisibility(View.VISIBLE);
        } else if (overlay.getVisibility()!=View.VISIBLE ) { //NB: stop game and show menu if not shown
            overlay.setVisibility(View.VISIBLE);
            mHandler.removeCallbacks(mRunnable); //stop the callback loop
        } else {
            // if instructions shown , offer close dialog
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Exit ?"); //TODO add app name?
            alertDialogBuilder.setMessage("Click yes to exit!").setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

    }


}
