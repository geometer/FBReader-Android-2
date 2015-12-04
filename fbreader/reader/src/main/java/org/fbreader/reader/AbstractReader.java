/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.fbreader.reader;

import java.util.Map;
import java.util.HashMap;

import org.fbreader.common.options.*;
import org.fbreader.util.Boolean3;

import org.geometerplus.fbreader.book.IBookCollection;
import org.geometerplus.fbreader.book.Book;

public abstract class AbstractReader implements IBookCollection.Listener<Book> {
	static abstract public class Action<T extends AbstractReader> {
		protected final T Reader;

		protected Action(T reader) {
			Reader = reader;
		}

		public boolean isVisible() {
			return true;
		}

		public boolean isEnabled() {
			return isVisible();
		}

		public Boolean3 isChecked() {
			return Boolean3.UNDEFINED;
		}

		public final boolean checkAndRun(Object ... params) {
			if (isEnabled()) {
				run(params);
				return true;
			}
			return false;
		}

		abstract protected void run(Object ... params);
	}

	private final HashMap<String,Action> myIdToActionMap = new HashMap<String,Action>();

	public final MiscOptions MiscOptions = new MiscOptions();
	public final PageTurningOptions PageTurningOptions = new PageTurningOptions();
	public final SyncOptions SyncOptions = new SyncOptions();

	private int myActionCount = 0;

	public final void addAction(String actionId, Action action) {
		myIdToActionMap.put(actionId, action);
	}

	public final void removeAction(String actionId) {
		myIdToActionMap.remove(actionId);
	}

	public final boolean runAction(String actionId, Object ... params) {
		final Action action = actionId != null ? myIdToActionMap.get(actionId) : null;
		if (action == null) {
			return false;
		}

		action.checkAndRun(params);
		++myActionCount;
		return true;
	}

	public final boolean isActionVisible(String actionId) {
		final Action action = myIdToActionMap.get(actionId);
		return action != null && action.isVisible();
	}

	public final boolean isActionEnabled(String actionId) {
		final Action action = myIdToActionMap.get(actionId);
		return action != null && action.isEnabled();
	}

	public final Boolean3 isActionChecked(String actionId) {
		final Action action = myIdToActionMap.get(actionId);
		return action != null ? action.isChecked() : Boolean3.UNDEFINED;
	}

	public abstract Book getCurrentBook();

	public abstract void storePosition();

	protected final void resetActionCount() {
		myActionCount = 0;
	}

	public final int getActionCount() {
		return myActionCount;
	}

	// method from IBookCollection.Listener
	public void onBuildEvent(IBookCollection.Status status) {
	}
}
