package org.geometerplus.fbreader.plugin.base.reader;

import org.fbreader.plugin.format.base.R;

import android.content.Context;
import android.text.*;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

public class PercentEditor extends LinearLayout {
	public interface ChangeListener {
		void onPercentChanged();
	}

	private ChangeListener myListener;

	public void setListener(ChangeListener c) {
		myListener = c;
	}

	public PercentEditor(Context context) {
		super(context);
		build(context);
	}

	public PercentEditor(Context context, AttributeSet attrs) {
		super(context, attrs);
		build(context);
	}

	private void build(Context context) {
		final LayoutInflater inflater =
			(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.fmt_percent_editor, this, true);
	}

	private int myMin = 0;
	private int myMax = 49;

	private EditText myEdit;

	public void init(String text, int value, int min, int max) {
		myMin = min;
		myMax = max;
		TextView type = (TextView) findViewById(R.id.fmt_crop_type);
		if (text != null) {
			type.setVisibility(View.VISIBLE);
			type.setText(text);
		} else {
			type.setVisibility(View.GONE);
		}
		myEdit = (EditText) findViewById(R.id.fmt_crop_edit);
		myEdit.setText(Integer.toString(value));
		myEdit.setFilters(new InputFilter[] { new InputFilterMinMax(0, myMax) });
		final ImageButton inc = (ImageButton) findViewById(R.id.fmt_crop_inc);
		final ImageButton dec = (ImageButton) findViewById(R.id.fmt_crop_dec);
		myEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (myListener != null) {
					myListener.onPercentChanged();
				}
				int cur = getValue();
				inc.setEnabled(cur < myMax);
				dec.setEnabled(cur > myMin);
			}
		});
		inc.setEnabled(value < max);
		dec.setEnabled(value > min);
		inc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int cur = getValue();
				if (cur < myMax) {
					cur = cur + 1;
					myEdit.setText(Integer.toString(cur));
				}
				if (myListener != null) {
					myListener.onPercentChanged();
				}
				inc.setEnabled(cur < myMax);
				dec.setEnabled(cur > myMin);
			}
		});
		dec.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int cur = getValue();
				if (cur > myMin) {
					cur = cur - 1;
					myEdit.setText(Integer.toString(cur));
				}
				inc.setEnabled(cur < myMax);
				dec.setEnabled(cur > myMin);
				if (myListener != null) {
					myListener.onPercentChanged();
				}
			}
		});
	}

	public int getValue() {
		if ("".equals(myEdit.getText().toString())) {
			return 0;
		}
		int res = Integer.parseInt(myEdit.getText().toString());
		return res > myMin ? res : myMin;
	}

	public void setValue(int v) {
		if (v < myMin) {
			v = myMin;
		}
		if (v > myMax) {
			v = myMax;
		}
		myEdit.setText(Integer.toString(v));
	}
}
