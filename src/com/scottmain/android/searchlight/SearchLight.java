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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;

public class SearchLight extends FragmentActivity implements PreviewSurface.Callback, ModeDialogFragment.ModeDialogListener {
	//private final static String TAG = "SearchLight";
	private final static String MODE_TYPE = "mode_type";
	ImageButton bulb;
	LightSwitch mLightswitch;
	
	TransitionDrawable mDrawable;
	PreviewSurface mSurface;
	boolean on = false;
	boolean paused = false;
	boolean skipAnimate = false;
	boolean mSystemUiVisible = true;
	int mCurrentMode = R.id.mode_lightbulb;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mode; // viewing mode
        
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
        case R.id.mode_blackout:
        	setContentView(R.layout.black);
        	mCurrentMode = R.id.mode_blackout;
        	break;
        case R.id.mode_viewfinder:
        	setContentView(R.layout.viewfinder);
        	mCurrentMode = R.id.mode_viewfinder;
        	break;
        case R.id.mode_lightswitch:
        	setContentView(R.layout.lightswitch);
        	mCurrentMode = R.id.mode_lightswitch;
        	mLightswitch = (LightSwitch) findViewById(R.id.switchbutton);
        	mLightswitch.setChecked(true);
        	mLightswitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton button, boolean isChecked) {
					if (isChecked){
						turnOn();
					} else {
						turnOff();
					}
				}
        		
        	});
        	
        	
        	break;
        case R.id.mode_lightbulb:
        default:
            setContentView(R.layout.main);
            mCurrentMode = R.id.mode_lightbulb;
        	break;
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
    	    mSurface.lightOn();
    	    // Update UI
    	    switch (mCurrentMode) {
    	    case R.id.mode_lightbulb:
    	    case R.id.mode_viewfinder:
        	    mDrawable.startTransition(200);
        	    break;
    	    case R.id.mode_lightswitch:
            	mLightswitch.setChecked(true);
        	    break;
    	    }
    	}
    }
    
    private void turnOff() {
    	if (on) {
	        on = false;
    	    mSurface.lightOff();
    	    // Update UI
    	    switch (mCurrentMode) {
    	    case R.id.mode_lightbulb:
    	    case R.id.mode_viewfinder:
    	        mDrawable.reverseTransition(300);
        	    break;
    	    case R.id.mode_lightswitch:
            	mLightswitch.setChecked(false);
        	    break;
    	    }
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
		// kill any ongoing transition so it's not still finishing when we resume
		mDrawable.resetTransition();
		findViewById(R.id.surface).invalidate();

		// Save the current mode so it's not lost when process stops
		SharedPreferences modePreferences = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = modePreferences.edit();
		editor.putInt(MODE_TYPE, mCurrentMode);
		editor.commit();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

        int mode = getIntent().getIntExtra(MODE_TYPE, R.id.mode_lightbulb);
        if (hasFocus && !skipAnimate && mode == R.id.mode_blackout) {
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

	/** In case a device has a MENU button, show the mode dialog when it's pressed */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    showModeDialog(findViewById(R.id.button_settings));
	    return true;
	}
	

	/** Implement the ModeDialogFragment's callback interface method */
	@Override
	public void onModeClick(int which) {
		if (mCurrentMode == which) return;

		skipAnimate = true;
	    if (which != -1) {
	        Intent intent = new Intent(this, SearchLight.class);
	        intent.putExtra(MODE_TYPE, which);
	        mSurface.releaseCamera();
	        startActivity(intent);
	        finish();
	    }
	}
	
	/** Call this to show the dialog with different light modes */
	public void showModeDialog(View v) {
		int currentMode = getIntent().getIntExtra(MODE_TYPE, R.id.mode_lightbulb);
	    DialogFragment newFragment = ModeDialogFragment.newInstance(currentMode);
	    newFragment.show(getSupportFragmentManager(), "mode_dialog");
	}

	public void cameraReady() {
		turnOn();
	}
	
	public void cameraNotAvailable() {
		showDialog(R.id.dialog_camera_na);
	}
	
}