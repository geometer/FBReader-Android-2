/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.tree;

import java.util.*;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.*;
import android.widget.ListView;

import org.geometerplus.fbreader.tree.FBTree;

import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer;
import org.geometerplus.android.util.*;

import org.fbreader.common.android.FBListActivity;
import org.fbreader.reader.android.UIMessageUtil;
import org.geometerplus.zlibrary.ui.android.R;

public abstract class TreeActivity<T extends FBTree> extends FBListActivity implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {
	private static final String OPEN_TREE_ACTION = "android.fbreader.action.OPEN_TREE";

	public static final String TREE_KEY_KEY = "TreeKey";
	public static final String SELECTED_TREE_KEY_KEY = "SelectedTreeKey";
	public static final String HISTORY_KEY = "HistoryKey";

	public final AndroidImageSynchronizer ImageSynchronizer = new AndroidImageSynchronizer(this);

	private T myCurrentTree;
	// we store the key separately because
	// it will be changed in case of myCurrentTree.removeSelf() call
	private FBTree.Key myCurrentKey;
	private final List<FBTree.Key> myHistory =
		Collections.synchronizedList(new ArrayList<FBTree.Key>());

	private volatile MenuItem mySearchItem;

	@Override
	protected int layoutId() {
		return R.layout.tree_activity;
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));

		getListView().setOnItemClickListener(this);
		getListView().setOnItemLongClickListener(this);
	}

	@Override
	protected void onDestroy() {
		ImageSynchronizer.clear();

		super.onDestroy();
	}

	protected T getCurrentTree() {
		return myCurrentTree;
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);

		if (OPEN_TREE_ACTION.equals(intent.getAction())) {
			runOnUiThread(new Runnable() {
				public void run() {
					init(intent);
				}
			});
		}
	}

	protected abstract T getTreeByKey(FBTree.Key key);
	public abstract boolean isTreeSelected(FBTree tree);

	protected boolean isTreeInvisible(FBTree tree) {
		return false;
	}

	@Override
	public void onBackPressed() {
		final MenuItem searchItem = mySearchItem;
		if (searchItem != null && searchItem.isVisible() && searchItem.isEnabled()) {
			final SearchView searchView = (SearchView)mySearchItem.getActionView();
			if (!searchView.isIconified()) {
				searchView.setIconified(true);
				searchView.setIconified(true);
				searchView.setQuery(defaultSearchQuery(), false);
				return;
			}
		}

		FBTree parent = null;
		synchronized (myHistory) {
			while (parent == null && !myHistory.isEmpty()) {
				parent = getTreeByKey(myHistory.remove(myHistory.size() - 1));
			}
		}
		if (parent == null && myCurrentTree != null) {
			parent = myCurrentTree.Parent;
		}
		if (parent != null && !isTreeInvisible(parent)) {
			openTree(parent, myCurrentTree, false);
		} else {
			super.onBackPressed();
		}
	}

	// TODO: change to protected
	public void openTree(final FBTree tree) {
		openTree(tree, null, true);
	}

	public void clearHistory() {
		runOnUiThread(new Runnable() {
			public void run() {
				myHistory.clear();
			}
		});
	}

	protected void onCurrentTreeChanged() {
	}

	private void openTree(final FBTree tree, final FBTree treeToSelect, final boolean storeInHistory) {
		switch (tree.getOpeningStatus()) {
			case WAIT_FOR_OPEN:
			case ALWAYS_RELOAD_BEFORE_OPENING:
				final String messageKey = tree.getOpeningStatusMessage();
				if (messageKey != null) {
					UIUtil.createExecutor(TreeActivity.this, messageKey).execute(
						new Runnable() {
							public void run() {
								tree.waitForOpening();
							}
						},
						new Runnable() {
							public void run() {
								openTreeInternal(tree, treeToSelect, storeInHistory);
							}
						}
					);
				} else {
					tree.waitForOpening();
					openTreeInternal(tree, treeToSelect, storeInHistory);
				}
				break;
			default:
				openTreeInternal(tree, treeToSelect, storeInHistory);
				break;
		}
	}

	protected void init(Intent intent) {
		final FBTree.Key key = (FBTree.Key)intent.getSerializableExtra(TREE_KEY_KEY);
		final FBTree.Key selectedKey = (FBTree.Key)intent.getSerializableExtra(SELECTED_TREE_KEY_KEY);
		myCurrentTree = getTreeByKey(key);
		// not myCurrentKey = key
		// because key might be null
		myCurrentKey = myCurrentTree.getUniqueKey();
		final TreeAdapter adapter = getTreeAdapter();
		adapter.replaceAll(myCurrentTree.subtrees(), false);
		setTitleAndSubtitle(myCurrentTree.getTreeTitle());
		final FBTree selectedTree =
			selectedKey != null ? getTreeByKey(selectedKey) : adapter.getFirstSelectedItem();
		final int index = adapter.getIndex(selectedTree);
		if (index != -1) {
			getListView().post(new Runnable() {
				public void run() {
					getListView().setSelection(index);
				}
			});
		}

		myHistory.clear();
		final ArrayList<FBTree.Key> history =
			(ArrayList<FBTree.Key>)intent.getSerializableExtra(HISTORY_KEY);
		if (history != null) {
			myHistory.addAll(history);
		}
		onCurrentTreeChanged();
	}

	private void openTreeInternal(FBTree tree, FBTree treeToSelect, boolean storeInHistory) {
		switch (tree.getOpeningStatus()) {
			case READY_TO_OPEN:
			case ALWAYS_RELOAD_BEFORE_OPENING:
				if (storeInHistory && !myCurrentKey.equals(tree.getUniqueKey())) {
					myHistory.add(myCurrentKey);
				}
				onNewIntent(new Intent(this, getClass())
					.setAction(OPEN_TREE_ACTION)
					.putExtra(TREE_KEY_KEY, tree.getUniqueKey())
					.putExtra(
						SELECTED_TREE_KEY_KEY,
						treeToSelect != null ? treeToSelect.getUniqueKey() : null
					)
					.putExtra(HISTORY_KEY, new ArrayList<FBTree.Key>(myHistory))
				);
				break;
			case CANNOT_OPEN:
				UIMessageUtil.showErrorMessage(TreeActivity.this, tree.getOpeningStatusMessage());
				break;
		}
	}

	protected final TreeAdapter getTreeAdapter() {
		return (TreeAdapter)getListAdapter();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search_only, menu);

		mySearchItem = menu.findItem(R.id.menu_search_item);
		final SearchView searchView = (SearchView)mySearchItem.getActionView();
		searchView.setQuery(defaultSearchQuery(), false);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String query) {
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				query = query.trim();
				if (!"".equals(query)) {
					doSearch(query);
					invalidateOptionsMenu();
				}
				return false;
			}
		});

		return true;
	}

	public final void openSearchItem() {
		final MenuItem searchItem = mySearchItem;
		if (searchItem != null && searchItem.isVisible() && searchItem.isEnabled()) {
			final SearchView searchView = (SearchView)mySearchItem.getActionView();
			searchView.setIconified(false);
			searchView.requestFocus();
		}
	}

	protected abstract String defaultSearchQuery();
	protected abstract void doSearch(String query);
}
