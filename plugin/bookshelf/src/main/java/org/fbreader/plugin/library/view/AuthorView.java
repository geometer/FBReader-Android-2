package org.fbreader.plugin.library.view;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextView;

import org.fbreader.plugin.library.R;

public final class AuthorView extends CardView {
	private static final Object NULL_VIEW = new Object();

	public AuthorView(Context context) {
		super(context);
	}

	public AuthorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AuthorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private Object myTitleView;
	public TextView titleView() {
		if (myTitleView == null) {
			myTitleView = findViewById(R.id.bks_author_title);
			if (myTitleView == null) {
				myTitleView = NULL_VIEW;
			}
		}
		return myTitleView != NULL_VIEW ? (TextView)myTitleView : null;
	}
}
