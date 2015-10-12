package org.fbreader.plugin.library;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.geometerplus.fbreader.book.*;

class BookPopupWindow extends PopupWindow {
	private final LibraryActivity myActivity;
	private final Book myBook;
	private final SparseArray<BookActionMenu.Action> myExtraActions;

	BookPopupWindow(LibraryActivity activity, Book book, SparseArray<BookActionMenu.Action> extraActions) {
		super(activity);

		myActivity = activity;
		myBook = book;
		myExtraActions = extraActions;

		final View popupView = activity.getLayoutInflater().inflate(
			R.layout.bks_book_popup, null
		);
		final View mainView = activity.mainView();
		final View shadowView = activity.findViewById(R.id.bks_library_popup_shadow);
		final ImageView coverView = (ImageView)popupView.findViewById(R.id.bks_book_popup_cover);
		final int inch = (int)TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_IN, 1, activity.getResources().getDisplayMetrics()
		);
		final int width = Math.min(4 * inch, mainView.getWidth() * 9 / 10);
		final int height;
		if (coverView != null) {
			height = Math.min(3 * width / 4, mainView.getHeight() * 9 / 10);
		} else {
			height = Math.min(3 * inch, mainView.getHeight() * 9 / 10);
		}
		setContentView(popupView);
		setWidth(width);
		setHeight(height);

		setOutsideTouchable(false);

		final TextView headerView = (TextView)popupView.findViewById(R.id.bks_book_popup_header);
		if (headerView != null) {
			final StringBuilder text = new StringBuilder();
			for (Author author : book.authors()) {
				text.append("<p><i>").append(author.DisplayName).append("</i></p>");
			}
			final SeriesInfo seriesInfo = book.getSeriesInfo();
			if (seriesInfo != null) {
				text.append("<p>");
				text.append(seriesInfo.Series.getTitle());
				if (seriesInfo.Index != null) {
					text.append(", #").append(seriesInfo.Index.toPlainString());
				}
				text.append("</p>");
			}
			text.append("<h3>").append(book.getTitle()).append("</h3>");
			headerView.setText(Html.fromHtml(text.toString()));
		}

		final TextView descriptionView =
			(TextView)popupView.findViewById(R.id.bks_book_popup_description);
		if (descriptionView != null) {
			final String description = activity.Collection.getDescription(book);
			if (description != null) {
				descriptionView.setText(Html.fromHtml(description));
				descriptionView.setMovementMethod(new LinkMovementMethod());
			}
		}

		if (coverView != null) {
			coverView.setVisibility(View.GONE);
			BookUtil.retrieveCover(activity, book, new BookUtil.BitmapRunnable() {
				public void run(final Bitmap bmp) {
					if (bmp == null) {
						return;
					}
					myActivity.runOnUiThread(new Runnable() {
						public void run() {
							coverView.setImageBitmap(bmp);
							coverView.setVisibility(View.VISIBLE);
						}
					});
				}
			});
		}

		setOnDismissListener(new PopupWindow.OnDismissListener() {
			public void onDismiss() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					setAlpha(mainView, 1.0f);
				}
				shadowView.setVisibility(View.GONE);
				BookUtil.resetPopup();
			}
		});

		// setup buttons
		popupView.findViewById(R.id.bks_book_popup_cancel).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				dismiss();
			}
		});
		popupView.findViewById(R.id.bks_book_popup_read).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				BookUtil.openBook(myActivity, myBook);
			}
		});

		shadowView.setLayoutParams(new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
		shadowView.setVisibility(View.VISIBLE);
		final View anchorView = shadowView.findViewById(R.id.bks_library_anchor);

		popupView.findViewById(R.id.bks_book_popup_more).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				new BookActionMenu(myActivity, BookPopupWindow.this, myBook, anchorView, R.menu.more, myExtraActions).show();
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setShadow();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setAlpha(mainView, 0.3f);
		}
	}

	void showAtCenter() {
		showAtLocation(myActivity.mainView(), Gravity.CENTER, 0, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void setShadow() {
		setElevation(TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP, 10, myActivity.getResources().getDisplayMetrics()
		));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setAlpha(View view, float alpha) {
		view.setAlpha(alpha);
	}
}
