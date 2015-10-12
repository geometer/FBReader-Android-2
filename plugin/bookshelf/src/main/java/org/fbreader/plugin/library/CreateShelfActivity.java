package org.fbreader.plugin.library;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;

public class CreateShelfActivity extends Activity {
	static final String NEW_SHELF_TITLE_KEY = "fbreader.new.shelf_title";

	private volatile TextView myOkButton;
	private volatile EditText myEditor;
	private volatile Timer myOkButtonUpdater;

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		ActivityUtil.setup(this, true);

		setContentView(R.layout.bks_shelf_creator);
		setResult(RESULT_CANCELED);

		final Intent intent = getIntent();

		myEditor = (EditText)findViewById(R.id.bks_shelf_creator_label);

		myOkButton = (TextView)findViewById(R.id.bks_shelf_creator_ok);
		myOkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				intent.putExtra(NEW_SHELF_TITLE_KEY, myEditor.getText().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		updateOkButton();

		findViewById(R.id.bks_shelf_creator_cancel).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
			}
		});
	}

	private void updateOkButton() {
		myOkButton.setEnabled(myEditor.getText().length() > 0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (myOkButtonUpdater == null) {
			myOkButtonUpdater = new Timer();
			myOkButtonUpdater.schedule(new TimerTask() {
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							updateOkButton();
						}
					});
				}
			}, 0, 100);
		}

		myEditor.requestFocus();
	}

	@Override
	protected void onPause() {
		if (myOkButtonUpdater != null) {
			myOkButtonUpdater.cancel();
			myOkButtonUpdater.purge();
			myOkButtonUpdater = null;
		}
		super.onPause();
	}

	// fix for NPE in button menu call on LG devices
	@Override
	public boolean onKeyDown(int code, KeyEvent event) {
		if (code == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
			return true;
		} else {
			return super.onKeyDown(code, event);
		}
	}

	@Override
	public boolean onKeyUp(int code, KeyEvent event) {
		if (code == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
			openOptionsMenu();
			return true;
		} else {
			return super.onKeyUp(code, event);
		}
	}
}
