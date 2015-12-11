package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class ZoomModeDialog extends OptionDialog {
	private PercentEditor myPageEdit;
	private PercentEditor myScreenEdit;
	private PluginView myPluginView;

	private int myZoomMode;
	private int myZoomPercent;
	
	public ZoomModeDialog(Context context, PluginView.ZoomMode mode) {
		super(context);
		myZoomMode = mode.Mode;
		myZoomPercent = mode.Percent;
	}
	
	protected int layoutId() {
		return R.layout.fmt_zoom_mode;
	}
	protected int titleId() {
		return R.string.zoomMode;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myPluginView = ViewHolder.getInstance().getView();

		myPageEdit = (PercentEditor)findViewById(R.id.fmt_page_percent_editor);
		myScreenEdit = (PercentEditor)findViewById(R.id.fmt_screen_percent_editor);
		PluginView.PDFPosition pos = myPluginView.getPosition();
		myScreenEdit.init(null, (int)(pos.Zoom * 100), 100, 999);
		myPageEdit.init(null, (int)(pos.PageZoom * 100), 10, 999);
		myScreenEdit.setListener(this);
		myPageEdit.setListener(this);

		RadioGroup group = (RadioGroup)findViewById(R.id.fmt_group);
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.fmt_screen_percent) {
					myScreenEdit.setVisibility(View.VISIBLE);
				} else {
					myScreenEdit.setVisibility(View.GONE);
				}
				if (checkedId == R.id.fmt_page_percent) {
					myPageEdit.setVisibility(View.VISIBLE);
				} else {
					myPageEdit.setVisibility(View.GONE);
				}
				if (checkedId == R.id.fmt_free_zoom) {
					myZoomMode = PluginView.ZoomMode.FREE_ZOOM;
					myZoomPercent = (int)(myPluginView.getPosition().Zoom * 100);
				} else if (checkedId == R.id.fmt_fit_page) {
					myZoomMode = PluginView.ZoomMode.FIT_PAGE;
					myZoomPercent = 0;
				} else if (checkedId == R.id.fmt_fit_width) {
					myZoomMode = PluginView.ZoomMode.FIT_WIDTH;
					myZoomPercent = 0;
				} else if (checkedId == R.id.fmt_fit_height) {
					myZoomMode = PluginView.ZoomMode.FIT_HEIGHT;
					myZoomPercent = 0;
				} else if (checkedId == R.id.fmt_screen_percent) {
					myZoomMode = PluginView.ZoomMode.SCREEN_ZOOM;
					myZoomPercent = (int)(myPluginView.getPosition().Zoom * 100);
					myScreenEdit.setValue(myZoomPercent);
				} else if (checkedId == R.id.fmt_page_percent) {
					myZoomMode = PluginView.ZoomMode.PAGE_ZOOM;
					myZoomPercent = (int)(myPluginView.getPosition().PageZoom * 100);
					myPageEdit.setValue(myZoomPercent);
				}
				myPluginView.setZoomMode(new PluginView.ZoomMode(myZoomMode, myZoomPercent));

			}
		});

		((RadioButton)findViewById(R.id.fmt_free_zoom)).setText(getContext().getResources().getString(R.string.free));
		((TextView)findViewById(R.id.fmt_fixed_text)).setText(getContext().getResources().getString(R.string.fixed));
		((RadioButton)findViewById(R.id.fmt_fit_page)).setText(getContext().getResources().getString(R.string.fitPage));
		((RadioButton)findViewById(R.id.fmt_fit_width)).setText(getContext().getResources().getString(R.string.fitWidth));
		((RadioButton)findViewById(R.id.fmt_fit_height)).setText(getContext().getResources().getString(R.string.fitHeight));
		((RadioButton)findViewById(R.id.fmt_screen_percent)).setText(getContext().getResources().getString(R.string.screenPercent));
		((RadioButton)findViewById(R.id.fmt_page_percent)).setText(getContext().getResources().getString(R.string.pagePercent));

		switch (myZoomMode) {
			case PluginView.ZoomMode.FREE_ZOOM:
				group.check(R.id.fmt_free_zoom);
				break;
			case PluginView.ZoomMode.FIT_PAGE:
				group.check(R.id.fmt_fit_page);
				break;
			case PluginView.ZoomMode.FIT_WIDTH:
				group.check(R.id.fmt_fit_width);
				break;
			case PluginView.ZoomMode.FIT_HEIGHT:
				group.check(R.id.fmt_fit_height);
				break;
			case PluginView.ZoomMode.SCREEN_ZOOM:
				group.check(R.id.fmt_screen_percent);
				break;
			case PluginView.ZoomMode.PAGE_ZOOM:
				group.check(R.id.fmt_page_percent);
				break;
			default:
				break;
		}
	}
	
	@Override
	public void onPercentChanged() {
		switch (myZoomMode) {
			case PluginView.ZoomMode.SCREEN_ZOOM:
				myZoomPercent = myScreenEdit.getValue();
				myPluginView.setZoomMode(new PluginView.ZoomMode(myZoomMode, myZoomPercent));
				break;
			case PluginView.ZoomMode.PAGE_ZOOM:
				myZoomPercent = myPageEdit.getValue();
				myPluginView.setZoomMode(new PluginView.ZoomMode(myZoomMode, myZoomPercent));
				break;
			default:
				break;
		}
	}

}
