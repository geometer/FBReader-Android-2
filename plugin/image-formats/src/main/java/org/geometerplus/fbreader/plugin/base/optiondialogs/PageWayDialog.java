package org.geometerplus.fbreader.plugin.base.optiondialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

public class PageWayDialog extends OptionDialog {
	private RadioButton myHButton;
	private RadioButton myVButton;

	private boolean myHorizontal;

	public PageWayDialog(Context context, PluginView view) {
		super(context, view);
		myHorizontal = view.isHorizontalFirst();
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

		setState(myHorizontal);

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
		myView.setHorizontalFirst(myHorizontal);
		super.onStop();
	}

	private void setState(boolean horizontal) {
		myHorizontal = horizontal;
		myHButton.setChecked(horizontal);
		myVButton.setChecked(!horizontal);
	}
}
