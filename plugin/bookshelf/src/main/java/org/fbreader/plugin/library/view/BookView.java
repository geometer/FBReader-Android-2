package org.fbreader.plugin.library.view;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.fbreader.util.android.ViewUtil;
import org.fbreader.plugin.library.R;

public final class BookView extends CardView {
	public BookView(Context context) {
		super(context);
	}

	public BookView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BookView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public TextView authorsView() {
		return ViewUtil.findTextView(this, R.id.bks_book_authors);
	}

	public TextView titleView() {
		return ViewUtil.findTextView(this, R.id.bks_book_title);
	}

	public ImageView coverView() {
		return ViewUtil.findImageView(this, R.id.bks_book_cover);
	}

	public View favView() {
		return ViewUtil.findView(this, R.id.bks_book_favorite);
	}

	public View favContainerView() {
		return ViewUtil.findView(this, R.id.bks_book_favorite_container);
	}

	public View readButton() {
		return ViewUtil.findView(this, R.id.bks_book_read);
	}

	public View moreButton() {
		return ViewUtil.findView(this, R.id.bks_book_more);
	}

	public TextView statusView() {
		final TextView statusView = ViewUtil.findTextView(this, R.id.bks_book_status);
		if (statusView != null) {
			return statusView;
		}
		return ViewUtil.findTextView(this, R.id.bks_book_status_small);
	}
	public int progressTextId() {
		final View view = statusView();
		if (view != null && view.getId() == R.id.bks_book_status_small) {
			return R.string.read_progress_small;
		}
		return R.string.read_progress;
	}
}
