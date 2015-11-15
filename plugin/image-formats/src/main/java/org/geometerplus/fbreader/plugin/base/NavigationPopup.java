/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.plugin.base;

import android.view.View;
import android.widget.*;

import org.fbreader.md.widget.Slider;

import org.geometerplus.fbreader.plugin.base.reader.PluginView;
import org.geometerplus.fbreader.plugin.base.reader.ThumbnailView;
import org.geometerplus.fbreader.plugin.base.tree.TOCTree;

import org.fbreader.plugin.format.base.R;

final class NavigationPopup implements ThumbnailView.PageChangeListener {
	private NavigationWindow myWindow;
	private PluginView.PDFPosition myStartPosition;
	private Button myResetButton;
	private PluginView myPDFReader;
	private TextView myText;
	private Slider mySlider;
	private View myView;

	private ThumbnailView myThumbs;

	NavigationPopup(PluginView p) {
		myPDFReader = p;
	}

	public void runNavigation(FBReaderPluginActivity activity, RelativeLayout root, String buttonText) {
		createControlPanel(activity, root, buttonText);
		myStartPosition = myPDFReader.getPosition();
		myWindow.show();
		setupNavigation();
	}

	public void update(FBReaderPluginActivity activity) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				setupNavigation();
			}
		});
	}

	public void stopNavigation() {
		final NavigationWindow window = myWindow;
		if (window == null) {
			return;
		}

		window.hide();
		myWindow = null;
	}

	public void createControlPanel(FBReaderPluginActivity activity, RelativeLayout root, String buttonText) {
		if (myWindow != null && activity == myWindow.getActivity()) {
			return;
		}

		activity.getLayoutInflater().inflate(R.layout.fmt_navigation_panel, root, true);
		myWindow = (NavigationWindow)root.findViewById(R.id.fmt_navigation_panel);
		myView = myWindow.findViewById(R.id.fmt_navigation_layout);

		mySlider = (Slider)myView.findViewById(R.id.fmt_navigation_slider);
		myText = (TextView)myView.findViewById(R.id.fmt_navigation_text);

		mySlider.setOnValueChangedListener(new Slider.OnValueChangedListener() {
			private void gotoPage(int page) {
				myThumbs.setPage(page);
			}

			public void onValueChanged(int progress) {
				final int page = progress + 1;
				final int pagesNumber = mySlider.getMax() + 1;
				myText.setText(makeProgressText(page, pagesNumber));
				gotoPage(progress);
			}
		});

		myResetButton = (Button)myView.findViewById(R.id.fmt_navigation_reset_button);
		myResetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (myStartPosition != null) {
					myPDFReader.gotoPosition(myStartPosition);
				}
				setupNavigation();
			}
		});
		myResetButton.setText(buttonText);

		myThumbs = (ThumbnailView)myView.findViewById(R.id.fmt_thumbs);
		myThumbs.setListener(this);
	}

	private void setupNavigation() {
		final NavigationWindow window = myWindow;
		if (window == null) {
			return;
		}

		final Slider slider = (Slider)window.findViewById(R.id.fmt_navigation_slider);
		final TextView text = (TextView)window.findViewById(R.id.fmt_navigation_text);

		int max = myPDFReader.getPagesNum() - 1;
		int curr = myPDFReader.getCurPageNo();

		if (slider.getMax() != max || slider.getValue() != curr) {
			slider.setMax(max);
			slider.setValue(curr);
			text.setText(makeProgressText(curr + 1, max + 1));
		}

		myResetButton.setEnabled(true);
		myThumbs.setPage(myPDFReader.getCurPageNo());
	}

	private String makeProgressText(int page, int pagesNumber) {
		final StringBuilder builder = new StringBuilder();
		builder.append(page);
		builder.append("/");
		builder.append(pagesNumber);
		final TOCTree tocElement = myPDFReader.getCurrentTOCElement(page - 1);
		if (tocElement != null && tocElement.getText() != null) {
			builder.append("  ");
			builder.append(tocElement.getText());
		}
		return builder.toString();
	}

	@Override
	public void onPageChanged(int no) {
		mySlider.setValue(no);
		final int page = no + 1;
		final int pagesNumber = mySlider.getMax() + 1;
		myText.setText(makeProgressText(page, pagesNumber));
		myView.postInvalidate();
	}

	@Override
	public void onPageSelected(int no) {
		myPDFReader.gotoPage(no, false);
	}
}
