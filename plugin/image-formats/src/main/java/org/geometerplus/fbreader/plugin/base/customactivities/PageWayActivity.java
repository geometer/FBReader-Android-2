package org.geometerplus.fbreader.plugin.base.customactivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import org.fbreader.common.android.FBActivity;

import org.fbreader.plugin.format.base.R;

public class PageWayActivity extends FBActivity {
	private RadioButton myHButton;
	private RadioButton myVButton;

	@Override
	protected int layoutId() {
		return R.layout.fmt_page_way;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.pageWay);

		myHButton = (RadioButton)findViewById(R.id.fmt_horiz_check);
		myVButton = (RadioButton)findViewById(R.id.fmt_vert_check);

		setState(getIntent().getBooleanExtra("horiz", true));

		final View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setState(v == myHButton);
			}
		};
		myHButton.setOnClickListener(listener);
		myVButton.setOnClickListener(listener);
	}

	private void setState(boolean horizontal) {
		myHButton.setChecked(horizontal);
		myVButton.setChecked(!horizontal);
		setResult(RESULT_OK, new Intent().putExtra("horiz", horizontal));
	}
}
