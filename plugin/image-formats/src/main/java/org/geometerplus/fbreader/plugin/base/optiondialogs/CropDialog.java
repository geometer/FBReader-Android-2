package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;

import android.content.Context;
import android.os.Bundle;

public class CropDialog extends OptionDialog {
	private final DocumentHolder.CropInfo myCropInfo;

	private PercentEditor myTopEdit;
	private PercentEditor myBottomEdit;
	private PercentEditor myLeftEdit;
	private PercentEditor myRightEdit;
	
	public CropDialog(Context context, DocumentHolder.CropInfo cropInfo) {
		super(context, null);
		myCropInfo = cropInfo;
	}
	
	protected int layoutId() {
		return R.layout.fmt_cropeditor_dialog;
	}
	protected int titleId() {
		return R.string.crop;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myTopEdit = initPercentEditor(R.id.fmt_crop_top, R.string.top, myCropInfo.TopPercent);
		myBottomEdit = initPercentEditor(R.id.fmt_crop_bottom, R.string.bottom, myCropInfo.BottomPercent);
		myLeftEdit = initPercentEditor(R.id.fmt_crop_left, R.string.left, myCropInfo.LeftPercent);
		myRightEdit = initPercentEditor(R.id.fmt_crop_right, R.string.right, myCropInfo.RightPercent);

		ViewHolder.getInstance().getView().setDrawBorders(true);
	}

	@Override
	protected void onStop() {
		ViewHolder.getInstance().getView().setDrawBorders(false);
		super.onStop();
	}

	private PercentEditor initPercentEditor(int id, int resourceId, int value) {
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
