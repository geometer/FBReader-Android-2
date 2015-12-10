package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


public abstract class OptionDialog extends Dialog implements PercentEditor.ChangeListener {

	protected Intent myIntent;
	
	public OptionDialog(Context context, int themeResId, Intent i) {
		super(context, themeResId);
		myIntent = i;
	}
	
	protected abstract int layoutId();
	protected abstract int titleId();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(titleId());
		setContentView(layoutId());
	}

	@Override
	protected void onStop() {
		onPercentChanged();
		ViewHolder.getInstance().storeAll();
		super.onStop();
	}



}
