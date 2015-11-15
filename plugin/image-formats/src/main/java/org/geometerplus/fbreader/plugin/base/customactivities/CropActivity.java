package org.geometerplus.fbreader.plugin.base.customactivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.fbreader.common.android.FBActivity;

import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;
import org.fbreader.plugin.format.base.R;

public class CropActivity extends FBActivity implements PercentEditor.ChangeListener {
	private PercentEditor myTopEdit;
	private PercentEditor myBottomEdit;
	private PercentEditor myLeftEdit;
	private PercentEditor myRightEdit;

	@Override
	protected int layoutId() {
		return R.layout.fmt_cropeditor_dialog;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.crop);

		myTopEdit = initPercentEditor(R.id.fmt_crop_top, R.string.top, "top");
		myBottomEdit = initPercentEditor(R.id.fmt_crop_bottom, R.string.bottom, "bottom");
		myLeftEdit = initPercentEditor(R.id.fmt_crop_left, R.string.left, "left");
		myRightEdit = initPercentEditor(R.id.fmt_crop_right, R.string.right, "right");

		setResult(RESULT_OK, getIntent());

		ViewHolder.getInstance().getView().setDrawBorders(true);
	}

	private PercentEditor initPercentEditor(int id, int resourceId, String key) {
		final int value = getIntent().getIntExtra(key, 0);
		final PercentEditor editor = (PercentEditor)findViewById(id);
		editor.init(getResources().getString(resourceId), value, 0, 49);
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
		setResult(RESULT_OK, new Intent()
			.putExtra("top", myTopEdit.getValue())
			.putExtra("bottom", myBottomEdit.getValue())
			.putExtra("right", myRightEdit.getValue())
			.putExtra("left", myLeftEdit.getValue())
		);
	}
}
