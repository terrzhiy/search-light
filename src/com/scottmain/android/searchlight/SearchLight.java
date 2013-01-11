/*
Copyright 2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.scottmain.android.searchlight;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;

public class SearchLight extends Activity implements PreviewSurface.Callback {
	//private final static String TAG = "SearchLight";
	private final static String MODE_TYPE = "mode_type";
	ImageButton bulb;
	
	TransitionDrawable mDrawable;
	PreviewSurface mSurface;
	boolean on = false;
	boolean paused = false;
	boolean skipAnimate = false;
	boolean mSystemUiVisible = true;
	boolean mIsHC = false;
	boolean mIsJB = false;
	int mCurrentMode = R.id.mode_normal;
	
    /** Called when the activity is first created. */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)  // Suppress lint for ActionBar Apis
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mode; // viewing mode
        
        // Set up version bools
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        	mIsHC = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            	mIsJB = true;
            }
        }
        
        // When user selects mode from menu, there's a mode type
        mode = getIntent().getIntExtra(MODE_TYPE, 0);
        // When launched clean, there's no mode in the intent, so check preference
        if (mode == 0) {
            SharedPreferences modePreferences = getPreferences(Context.MODE_PRIVATE);
            mode = modePreferences.getInt(MODE_TYPE, 0);
            
            // Rewrite the intent to carry the desired mode
            Intent intent = getIntent();
	        intent.putExtra(MODE_TYPE, mode);
            setIntent(intent);
        }
        
        switch(mode) {
        case R.id.mode_black:
        	setContentView(R.layout.black);
        	mCurrentMode = R.id.mode_black;

            setSystemUiVisible(false);
        	break;
        case R.id.mode_viewfinder:
        	setContentView(R.layout.viewfinder);
        	mCurrentMode = R.id.mode_viewfinder;

        	setSystemUiVisible(false);
        	break;
        case R.id.mode_normal:
        default:
            setContentView(R.layout.main);
            mCurrentMode = R.id.mode_normal;
        	break;
        }
        
        // Remove icon and title from action bar
        if (mIsHC) {
        	ActionBar actionBar = getActionBar();
        	actionBar.setDisplayShowTitleEnabled(false);
        	actionBar.setDisplayShowHomeEnabled(false);
        }
        
        mSurface = (PreviewSurface) findViewById(R.id.surface);
        mSurface.setCallback(this);
        if (mode == R.id.mode_viewfinder) {
        	mSurface.setIsViewfinder();
        }
        
        bulb = (ImageButton) findViewById(R.id.button);
        mDrawable = (TransitionDrawable) bulb.getDrawable();
        mDrawable.setCrossFadeEnabled(true);
    }
    
    public void toggleLight(View v) {
    	if (on) {
    		turnOff();
    	} else {
    		turnOn();
    	}
    }
    
    private void turnOn() {
    	if (!on) {
    	    on = true;
    	    mDrawable.startTransition(200);
    	    mSurface.lightOn();
    	}
    }
    
    private void turnOff() {
    	if (on) {
	        on = false;
	        mDrawable.reverseTransition(300);
    	    mSurface.lightOff();
    	}
    }

	@Override
	protected void onPause() {
		super.onPause();
		turnOff();
		mSurface.releaseCamera();
		paused = true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (paused) {
			mSurface.initCamera();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		paused = false;

		// Save the current mode so it's not lost when process stops
		SharedPreferences modePreferences = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = modePreferences.edit();
		editor.putInt(MODE_TYPE, mCurrentMode);
		editor.commit();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

        int mode = getIntent().getIntExtra(MODE_TYPE, R.id.mode_normal);
        if (hasFocus && !skipAnimate && mode == R.id.mode_black) {
        	Button image = (Button) findViewById(R.id.toggleButton);
        	Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        	image.startAnimation(fadeOut);
        }
    	skipAnimate = false;
        
		if (hasFocus && paused) {
			mSurface.startPreview();
			paused = false;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_camera_na:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_camera_na)
			       .setCancelable(false)
			       .setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                SearchLight.this.finish();
			           }
			       });
			return builder.create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}
	
	

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// TODO Auto-generated method stub
		super.onOptionsMenuClosed(menu);
		skipAnimate = true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
	    switch (item.getItemId()) {
	    case R.id.black:
	        intent = new Intent(this, SearchLight.class);
	        intent.putExtra(MODE_TYPE, R.id.mode_black);
	        mSurface.releaseCamera();
	        startActivity(intent);
	        finish();
	        return true;
	    case R.id.viewfinder:
	        intent = new Intent(this, SearchLight.class);
	        intent.putExtra(MODE_TYPE, R.id.mode_viewfinder);
	        mSurface.releaseCamera();
	        startActivity(intent);
	        finish();
	        return true;
	    case R.id.normal:
	        intent = new Intent(this, SearchLight.class);
	        intent.putExtra(MODE_TYPE, R.id.mode_normal);
	        mSurface.releaseCamera();
	        startActivity(intent);
	        finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        switch(getIntent().getIntExtra(MODE_TYPE, R.id.mode_normal)) {
        case R.id.mode_black:
			menu.findItem(R.id.black).setVisible(false);
			menu.findItem(R.id.normal).setVisible(true);
			menu.findItem(R.id.viewfinder).setVisible(true);
        	break;
        case R.id.mode_viewfinder:
			menu.findItem(R.id.black).setVisible(true);
			menu.findItem(R.id.normal).setVisible(true);
			menu.findItem(R.id.viewfinder).setVisible(false);
        	break;
        case R.id.mode_normal:
        default:
			menu.findItem(R.id.black).setVisible(true);
			menu.findItem(R.id.normal).setVisible(false);
			menu.findItem(R.id.viewfinder).setVisible(true);
        	break;
        }
		return super.onPrepareOptionsMenu(menu);
	}

	public void cameraReady() {
		turnOn();
	}
	
	public void cameraNotAvailable() {
		showDialog(R.id.dialog_camera_na);
	}
	
	

	
    /** Toggle whether the system UI (status bar / system bar) is visible.
     *  This also toggles the action bar visibility.
     * @param show True to show the system UI, false to hide it.
     */
	// Suppress lint because we don't use ActionBar unless mIsHC is true
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void setSystemUiVisible(boolean show) {
        mSystemUiVisible = show;
        Window window = getWindow();

        if (mIsHC) {
        // ******* POST HONEYCOMB HIDE UI *********
	        WindowManager.LayoutParams winParams = window.getAttributes();
	        View view = findViewById(android.R.id.content);
	        ActionBar actionBar = getActionBar();
	
	        if (show) {
	            // Show status bar (remove fullscreen flag)
	            window.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	            // Show system bar
	            view.setSystemUiVisibility(View.STATUS_BAR_VISIBLE);
	            // Show action bar
	            actionBar.show();
	        }
	        
	        if (!mIsJB) {
	    	    new TimeoutActionBarTask().execute(1);
	        }
	        window.setAttributes(winParams);
        } else {
        // ***** PRE HONEYCOMB HIDE UI ******
        	if (show) {
	            // Show status bar (remove fullscreen flag)
	            window.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	} else {
        		// Hide the status bar
	            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	            		WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	}
        }
    }
    

    
    
    boolean mIgnoreTouch = false;

    /* Bullshit timout to make UI hiding fluid for versions before JB */
    private class TimeoutSystemUITask extends AsyncTask<Integer,Integer,Integer> {
        /** The system calls this to perform work in a worker thread and
          * delivers it the parameters given to AsyncTask.execute() */
        protected Integer doInBackground(Integer...ints) {
        	mIgnoreTouch = true;
        	try {
        		// Pause 3 seconds before hiding UI
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return -1;
			}
            return 1;
        }
        
        /** The system calls this to perform work in the UI thread and delivers
          * the result from doInBackground() */
        // suppress lint for the setSystemUiVisibility because this isn't used pre-JB (let-alone pre-HC)
        @SuppressLint
        ("NewApi") protected void onPostExecute(Integer result) {
            Window window = getWindow();
            View view = findViewById(android.R.id.content);
            // Add fullscreen flag (hide status bar)
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // Hide system bar
            view.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
            // Hide the action bar in callback below
        	mIgnoreTouch = false;
        }
    }
    
    private class TimeoutActionBarTask extends AsyncTask<Integer,Integer,Integer> {
        /** The system calls this to perform work in a worker thread and
          * delivers it the parameters given to AsyncTask.execute() */
        protected Integer doInBackground(Integer...ints) {
        	mIgnoreTouch = true;
        	try {
        		// Pause 3 seconds before hiding UI
				Thread.sleep(2500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return -1;
			}
            return 1;
        }
        
        /** The system calls this to perform work in the UI thread and delivers
          * the result from doInBackground() */
        // Suppress lint because the TimoutActionBarTask isn't instantiated pre-HC
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        protected void onPostExecute(Integer result) {
        	getActionBar().hide();
        	new TimeoutSystemUITask().execute(1);
        	mIgnoreTouch = false;
        }
    }
	
}