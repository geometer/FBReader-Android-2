package org.fbreader.plugin.library.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.fbreader.plugin.library.R;

public class DrawerItem extends LinearLayout {
	private static final Object NULL_VIEW = new Object();

	public DrawerItem(Context context) {
		super(context);
	}

	public DrawerItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public DrawerItem(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public DrawerItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	private Object myTitleView;
	public TextView titleView() {
		if (myTitleView == null) {
			myTitleView = findViewById(R.id.bks_drawer_item_title);
			if (myTitleView == null) {
				myTitleView = NULL_VIEW;
			}
		}
		return myTitleView != NULL_VIEW ? (TextView)myTitleView : null;
	}

	private Object mySummaryView;
	public TextView summaryView() {
		if (mySummaryView == null) {
			mySummaryView = findViewById(R.id.bks_drawer_item_summary);
			if (mySummaryView == null) {
				mySummaryView = NULL_VIEW;
			}
		}
		return mySummaryView != NULL_VIEW ? (TextView)mySummaryView : null;
	}
}
