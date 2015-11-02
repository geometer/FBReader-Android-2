/*
 * Copyright (C) 2007-2014 Geometer Plus <contact@geometerplus.com>
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

package org.fbreader.plugin.library.report;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

import org.fbreader.plugin.library.FullActivity;
import org.fbreader.plugin.library.R;

public class BugReportActivity extends FullActivity {
	static final String STACKTRACE = "fbreader.stacktrace";

	@Override
	protected int layoutId() {
		return R.layout.bks_bug_report;
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		final StringBuilder reportText = new StringBuilder();

		reportText.append("Model:").append(Build.MODEL).append("\n");
		reportText.append("Device:").append(Build.DEVICE).append("\n");
		reportText.append("Product:").append(Build.PRODUCT).append("\n");
		reportText.append("Manufacturer:").append(Build.MANUFACTURER).append("\n");
		reportText.append("Version:").append(Build.VERSION.RELEASE).append("\n");
		reportText.append(getIntent().getStringExtra(STACKTRACE));

		final TextView reportTextView = (TextView)findViewById(R.id.bks_bug_report_text);
		reportTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
		reportTextView.setClickable(false);
		reportTextView.setLongClickable(false);

		final String versionName = getVersionName();
		reportTextView.append(getResources().getString(R.string.send_report, versionName));
		reportTextView.append("\n\n");
		reportTextView.append(reportText);

		findViewById(R.id.bks_bug_report_send_button).setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View view) {
					Intent sendIntent = new Intent(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "exception@fbreader.org" });
					sendIntent.putExtra(Intent.EXTRA_TEXT, reportText.toString());
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Bookshelf " + versionName + " exception report");
					sendIntent.setType("message/rfc822");
					startActivity(sendIntent);
					finish();
				}
			}
		);

		findViewById(R.id.bks_bug_report_cancel_button).setOnClickListener(
			new View.OnClickListener() {
				public void onClick(View view) {
					finish();
				}
			}
		);
	}

	private String getVersionName() {
		try {
			final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			return info.versionName + " (" + info.versionCode + ")";
		} catch (Exception e) {
			return "";
		}
	}
}
