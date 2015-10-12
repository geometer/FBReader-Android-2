package org.fbreader.plugin.library.view;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import org.fbreader.plugin.library.R;

public final class FileView extends CardView {
	private static final Object NULL_VIEW = new Object();

	public FileView(Context context) {
		super(context);
	}

	public FileView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FileView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private Object myNameView;
	public TextView nameView() {
		if (myNameView == null) {
			myNameView = findViewById(R.id.bks_file_name);
			if (myNameView == null) {
				myNameView = NULL_VIEW;
			}
		}
		return myNameView != NULL_VIEW ? (TextView)myNameView : null;
	}

	private Object myIconView;
	public ImageView iconView() {
		if (myIconView == null) {
			myIconView = findViewById(R.id.bks_file_icon);
			if (myIconView == null) {
				myIconView = NULL_VIEW;
			}
		}
		return myIconView != NULL_VIEW ? (ImageView)myIconView : null;
	}
}
