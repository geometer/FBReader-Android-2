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

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
	private final Context myContext;

	public ExceptionHandler(Context context) {
		myContext = context;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		final StringWriter stackTrace = new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));
		System.err.println(stackTrace);

		myContext.startActivity(
			new Intent(myContext, BugReportActivity.class)
				.putExtra(BugReportActivity.STACKTRACE, stackTrace.toString())
		);

		if (myContext instanceof Activity) {
			((Activity)myContext).finish();
		}

		Process.killProcess(Process.myPid());
		System.exit(10);
	}
}
