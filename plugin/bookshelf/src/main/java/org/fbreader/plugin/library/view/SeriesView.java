package org.fbreader.plugin.library.view;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import org.fbreader.plugin.library.R;

public final class SeriesView extends CardView {
	private static final Object NULL_VIEW = new Object();

	public SeriesView(Context context) {
		super(context);
	}

	public SeriesView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SeriesView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private Object myTitleView;
	public TextView titleView() {
		if (myTitleView == null) {
			myTitleView = findViewById(R.id.bks_series_title);
			if (myTitleView == null) {
				myTitleView = NULL_VIEW;
			}
		}
		return myTitleView != NULL_VIEW ? (TextView)myTitleView : null;
	}

	private Object myCoversView;
	public ImageView coversView() {
		if (myCoversView == null) {
			myCoversView = findViewById(R.id.bks_series_covers);
			if (myCoversView == null) {
				myCoversView = NULL_VIEW;
			}
		}
		return myCoversView != NULL_VIEW ? (ImageView)myCoversView : null;
	}
}
