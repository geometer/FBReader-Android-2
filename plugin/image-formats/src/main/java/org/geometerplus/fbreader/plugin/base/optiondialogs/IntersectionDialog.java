package org.geometerplus.fbreader.plugin.base.optiondialogs;

import org.fbreader.plugin.format.base.R;
import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;

import android.content.Context;
import android.os.Bundle;

public class IntersectionDialog extends OptionDialog {
	private volatile PluginView.IntersectionsHolder myIntersections;

	private PercentEditor myXEdit;
	private PercentEditor myYEdit;
	
	public IntersectionDialog(Context context, PluginView.IntersectionsHolder intersections) {
		super(context);
		myIntersections = intersections;
	}
	
	protected int layoutId() {
		return R.layout.fmt_intersections;
	}
	protected int titleId() {
		return R.string.intersections;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myXEdit = initPercentEditor(R.id.fmt_x_edit, R.string.x, myIntersections.XPercent);
		myYEdit = initPercentEditor(R.id.fmt_y_edit, R.string.y, myIntersections.YPercent);

		ViewHolder.getInstance().getView().setDrawIntersections(true);
	}
	
	@Override
	protected void onStop() {
		ViewHolder.getInstance().getView().setDrawIntersections(false);
		super.onStop();
	}

	private PercentEditor initPercentEditor(int id, int resourceId, int value) {
		final PercentEditor editor = (PercentEditor)findViewById(id);
		editor.init(getContext().getResources().getString(resourceId), value, 0, 99);
		editor.setListener(this);
		return editor;
	}

	@Override
	public void onPercentChanged() {
		myIntersections = new PluginView.IntersectionsHolder(
			myXEdit.getValue(),
			myYEdit.getValue()
		);
		ViewHolder.getInstance().getView().setIntersections(myIntersections);
	}
}
