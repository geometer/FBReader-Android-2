package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

public abstract class OptionDialog extends Dialog {
	public OptionDialog(Context context) {
		super(context, R.style.FBReaderMD_Dialog_Translucent);
	}
	
	protected abstract int layoutId();
	protected abstract int titleId();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId());
		final Toolbar toolbar = (Toolbar)findViewById(R.id.md_toolbar);
		toolbar.setTitle(titleId());
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				cancel();
			}
		});
		toolbar.setNavigationContentDescription(android.R.string.cancel);
	}

	@Override
	protected void onStop() {
		ViewHolder.getInstance().storeAll();
		super.onStop();
	}
}
