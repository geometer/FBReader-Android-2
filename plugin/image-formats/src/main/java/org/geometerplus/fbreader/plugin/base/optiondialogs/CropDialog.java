package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.document.DocumentHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

import android.content.Context;
import android.os.Bundle;

public class CropDialog extends OptionDialog implements PercentEditor.ChangeListener {
	private final DocumentHolder.CropInfo myCropInfo;

	private PercentEditor myTopEdit;
	private PercentEditor myBottomEdit;
	private PercentEditor myLeftEdit;
	private PercentEditor myRightEdit;

	public CropDialog(Context context, PluginView view) {
		super(context, view);
		myCropInfo = view.getDocument().getCropInfo();
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

		myView.setDrawBorders(true);
	}

	@Override
	protected void onStop() {
		onPercentChanged();
		myView.setDrawBorders(false);
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
		myView.getDocument().setCropInfo(
			myTopEdit.getValue(),
			myBottomEdit.getValue(),
			myLeftEdit.getValue(),
			myRightEdit.getValue()
		);
	}
}
