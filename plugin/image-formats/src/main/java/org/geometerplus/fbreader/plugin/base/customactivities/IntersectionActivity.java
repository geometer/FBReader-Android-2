package org.geometerplus.fbreader.plugin.base.customactivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.fbreader.common.android.FBActivity;

import org.geometerplus.fbreader.plugin.base.ViewHolder;
import org.geometerplus.fbreader.plugin.base.reader.PercentEditor;
import org.geometerplus.fbreader.plugin.base.reader.PluginView.IntersectionsHolder;

import org.fbreader.plugin.format.base.R;

public class IntersectionActivity extends FBActivity implements PercentEditor.ChangeListener {
	private PercentEditor myXEdit;
	private PercentEditor myYEdit;

	@Override
	protected int layoutId() {
		return R.layout.fmt_intersections;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.intersections);

		myXEdit = initPercentEditor(R.id.fmt_x_edit, R.string.x, "x");
		myYEdit = initPercentEditor(R.id.fmt_y_edit, R.string.y, "y");

		setResult(RESULT_OK, getIntent());
		ViewHolder.getInstance().getView().setDrawIntersections(true);
	}

	private PercentEditor initPercentEditor(int id, int resourceId, String key) {
		final int value = getIntent().getIntExtra(key, 10);
		final PercentEditor editor = (PercentEditor)findViewById(id);
		editor.init(getResources().getString(resourceId), value, 0, 99);
		editor.setListener(this);
		return editor;
	}

	@Override
	public void onPercentChanged() {
		final int x = myXEdit.getValue();
		final int y = myYEdit.getValue();
		ViewHolder.getInstance().getView().setIntersections(new IntersectionsHolder(x, y));
		setResult(RESULT_OK, new Intent().putExtra("x", x).putExtra("y", y));
	}
}
