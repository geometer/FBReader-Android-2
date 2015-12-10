package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.ViewHolder;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

public class PageWayDialog extends OptionDialog {
	private RadioButton myHButton;
	private RadioButton myVButton;
	
	public PageWayDialog(Context context, int themeResId, Intent i) {
		super(context, themeResId, i);
	}
	
	protected int layoutId() {
		return R.layout.fmt_page_way;
	}
	protected int titleId() {
		return R.string.pageWay;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myHButton = (RadioButton)findViewById(R.id.fmt_horiz_check);
		myVButton = (RadioButton)findViewById(R.id.fmt_vert_check);

		setState(myIntent.getBooleanExtra("horiz", true));

		final View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setState(v == myHButton);
			}
		};
		myHButton.setOnClickListener(listener);
		myVButton.setOnClickListener(listener);
	}
	
	@Override
	protected void onStop() {
		ViewHolder.getInstance().getView().setHorizontalFirst(myHButton.isChecked());
		super.onStop();
	}

	private void setState(boolean horizontal) {
		myHButton.setChecked(horizontal);
		myVButton.setChecked(!horizontal);
	}

	@Override
	public void onPercentChanged() {
	}
}
