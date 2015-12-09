package org.geometerplus.fbreader.plugin.base.customdialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


public class CropDialog extends Dialog implements PercentEditor.ChangeListener {

	private PercentEditor myTopEdit;
	private PercentEditor myBottomEdit;
	private PercentEditor myLeftEdit;
	private PercentEditor myRightEdit;
	
	private Intent myIntent;
	
	public CropDialog(Context context, int themeResId, Intent i) {
		super(context, themeResId);
		myIntent = i;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.crop);
		setContentView(R.layout.fmt_cropeditor_dialog);
		
		myTopEdit = initPercentEditor(R.id.fmt_crop_top, R.string.top, "top");
		myBottomEdit = initPercentEditor(R.id.fmt_crop_bottom, R.string.bottom, "bottom");
		myLeftEdit = initPercentEditor(R.id.fmt_crop_left, R.string.left, "left");
		myRightEdit = initPercentEditor(R.id.fmt_crop_right, R.string.right, "right");

		ViewHolder.getInstance().getView().setDrawBorders(true);
		
	}

	@Override
	protected void onStop() {
		onPercentChanged();
		ViewHolder.getInstance().getView().setDrawBorders(false);
		ViewHolder.getInstance().storeAll();
		super.onStop();
	}

	private PercentEditor initPercentEditor(int id, int resourceId, String key) {
		final int value = myIntent.getIntExtra(key, 0);
		final PercentEditor editor = (PercentEditor)findViewById(id);
		getContext().getResources();
		getContext().getResources().getString(resourceId);
		editor.init(getContext().getResources().getString(resourceId), value, 0, 49);
		editor.setListener(this);
		return editor;
	}

	@Override
	public void onPercentChanged() {
		ViewHolder.getInstance().getView().getDocument().setCropInfo(
				myTopEdit.getValue(),
				myBottomEdit.getValue(),
				myLeftEdit.getValue(),
				myRightEdit.getValue()
				);
	}


}
