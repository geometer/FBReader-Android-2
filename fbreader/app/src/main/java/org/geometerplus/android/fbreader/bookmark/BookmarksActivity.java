/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.bookmark;

import java.util.*;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;

import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView;

import org.geometerplus.zlibrary.core.util.MiscUtil;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.options.ZLStringOption;

import org.fbreader.md.MDActivity;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.book.*;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.*;

public class BookmarksActivity extends MDActivity implements IBookCollection.Listener<Book> {
	private static final int OPEN_ITEM_ID = 0;
	private static final int EDIT_ITEM_ID = 1;
	private static final int DELETE_ITEM_ID = 2;

	private TabLayout myTabLayout;
	private ViewPager myViewPager;

	private final Map<Integer,HighlightingStyle> myStyles =
		Collections.synchronizedMap(new HashMap<Integer,HighlightingStyle>());

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private volatile Book myBook;
	private volatile Bookmark myBookmark;

	private final Comparator<Bookmark> myComparator = new Bookmark.ByTimeComparator();

	private volatile BookmarksFragment myThisBookFragment = new BookmarksFragment();
	private volatile BookmarksFragment myAllBooksFragment = new BookmarksFragment();
	private volatile BookmarksFragment mySearchResultsFragment = new BookmarksFragment();
	private volatile BookmarksAdapter myThisBookAdapter;
	private volatile BookmarksAdapter myAllBooksAdapter;
	private volatile BookmarksAdapter mySearchResultsAdapter;

	private final ZLResource myResource = ZLResource.resource("bookmarksView");
	private final ZLStringOption myBookmarkSearchPatternOption =
		new ZLStringOption("BookmarkSearch", "Pattern", "");

	@Override
	protected int layoutId() {
		return R.layout.bookmarks;
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));

		setTitle("");

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
		manager.setOnCancelListener(null);

        myTabLayout = (TabLayout)findViewById(R.id.bookmarks_tab_layout);
        myViewPager = (ViewPager)findViewById(R.id.bookmarks_view_pager);
		final PagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
			@Override
			public int getCount() {
				return 3;
			}

			@Override
			public Fragment getItem(int position) {
				switch (position) {
					default:
					case 0:
						return myThisBookFragment;
					case 1:
						return myAllBooksFragment;
					case 2:
						return mySearchResultsFragment;
				}
			}

			@Override
			public CharSequence getPageTitle(int position) {
				final String key;
				switch (position) {
					default:
					case 0:
						key = "thisBook";
						break;
					case 1:
						key = "allBooks";
						break;
					case 2:
						key = "search";
						break;
				}
				return myResource.getResource(key).getValue();
			}
		};
		myViewPager.setAdapter(adapter);
		myTabLayout.setupWithViewPager(myViewPager);

		myTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				myViewPager.setCurrentItem(tab.getPosition(), false);
				setupSearchFragment(tab);
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				setupSearchFragment(tab);
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
			}

			private void setupSearchFragment(TabLayout.Tab tab) {
				final FragmentPagerAdapter adapter =
					(FragmentPagerAdapter)myViewPager.getAdapter();
				if (adapter.getItem(tab.getPosition()) == mySearchResultsFragment) {
					onSearchRequested();
				}
			}
		});

		myBook = FBReaderIntents.getBookExtra(getIntent(), myCollection);
		if (myBook == null) {
			finish();
		}
		myBookmark = FBReaderIntents.getBookmarkExtra(getIntent());
	}

	@Override
	protected void onStart() {
		super.onStart();

		myCollection.bindToService(this, new Runnable() {
			public void run() {
				if (myAllBooksAdapter != null) {
					return;
				}

				myThisBookAdapter = new BookmarksAdapter(myThisBookFragment, myBookmark != null);
				myAllBooksAdapter = new BookmarksAdapter(myAllBooksFragment, false);
				myCollection.addListener(BookmarksActivity.this);

				updateStyles();
				loadBookmarks();
			}
		});

		OrientationUtil.setOrientation(this, getIntent());
	}

	private void updateStyles() {
		synchronized (myStyles) {
			myStyles.clear();
			for (HighlightingStyle style : myCollection.highlightingStyles()) {
				myStyles.put(style.Id, style);
			}
		}
	}

	private final Object myBookmarksLock = new Object();

	private void loadBookmarks() {
		new Thread(new Runnable() {
			public void run() {
				synchronized (myBookmarksLock) {
					for (BookmarkQuery query = new BookmarkQuery(myBook, 50); ; query = query.next()) {
						final List<Bookmark> thisBookBookmarks = myCollection.bookmarks(query);
						if (thisBookBookmarks.isEmpty()) {
							break;
						}
						myThisBookAdapter.addAll(thisBookBookmarks);
						myAllBooksAdapter.addAll(thisBookBookmarks);
					}
					for (BookmarkQuery query = new BookmarkQuery(50); ; query = query.next()) {
						final List<Bookmark> allBookmarks = myCollection.bookmarks(query);
						if (allBookmarks.isEmpty()) {
							break;
						}
						myAllBooksAdapter.addAll(allBookmarks);
					}
				}
			}
		}).start();
	}

	private void updateBookmarks(final Book book) {
		new Thread(new Runnable() {
			public void run() {
				synchronized (myBookmarksLock) {
					final boolean flagThisBookTab = book.getId() == myBook.getId();
					final boolean flagSearchTab = mySearchResultsAdapter != null;

					final Map<String,Bookmark> oldBookmarks = new HashMap<String,Bookmark>();
					if (flagThisBookTab) {
						for (Bookmark b : myThisBookAdapter.bookmarks()) {
							oldBookmarks.put(b.Uid, b);
						}
					} else {
						for (Bookmark b : myAllBooksAdapter.bookmarks()) {
							if (b.BookId == book.getId()) {
								oldBookmarks.put(b.Uid, b);
							}
						}
					}
					final String pattern = myBookmarkSearchPatternOption.getValue().toLowerCase();

					for (BookmarkQuery query = new BookmarkQuery(book, 50); ; query = query.next()) {
						final List<Bookmark> loaded = myCollection.bookmarks(query);
						if (loaded.isEmpty()) {
							break;
						}
						for (Bookmark b : loaded) {
							final Bookmark old = oldBookmarks.remove(b.Uid);
							myAllBooksAdapter.replace(old, b);
							if (flagThisBookTab) {
								myThisBookAdapter.replace(old, b);
							}
							if (flagSearchTab && MiscUtil.matchesIgnoreCase(b.getText(), pattern)) {
								mySearchResultsAdapter.replace(old, b);
							}
						}
					}
					myAllBooksAdapter.removeAll(oldBookmarks.values());
					if (flagThisBookTab) {
						myThisBookAdapter.removeAll(oldBookmarks.values());
					}
					if (flagSearchTab) {
						mySearchResultsAdapter.removeAll(oldBookmarks.values());
					}
				}
			}
		}).start();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		OrientationUtil.setOrientation(this, intent);

		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
			return;
		}
		String pattern = intent.getStringExtra(SearchManager.QUERY);
		myBookmarkSearchPatternOption.setValue(pattern);

		final LinkedList<Bookmark> bookmarks = new LinkedList<Bookmark>();
		pattern = pattern.trim().toLowerCase();
		for (Bookmark b : myAllBooksAdapter.bookmarks()) {
			if (MiscUtil.matchesIgnoreCase(b.getText(), pattern)) {
				bookmarks.add(b);
			}
		}
		if (!bookmarks.isEmpty()) {
			final ListView resultsView = mySearchResultsFragment.getListView();
			if (mySearchResultsAdapter == null) {
				mySearchResultsAdapter = new BookmarksAdapter(mySearchResultsFragment, false);
				resultsView.setOnItemLongClickListener(mySearchResultsAdapter);
			} else {
				mySearchResultsAdapter.clear();
			}
			mySearchResultsAdapter.addAll(bookmarks);
		} else {
			UIMessageUtil.showErrorMessage(this, "bookmarkNotFound");
		}
	}

	@Override
	protected void onDestroy() {
		myCollection.unbind();
		super.onDestroy();
	}

	@Override
	public boolean onSearchRequested() {
		if (DeviceType.Instance().hasStandardSearchDialog()) {
			startSearch(myBookmarkSearchPatternOption.getValue(), true, null, false);
		} else {
			SearchDialogUtil.showDialog(this, BookmarksActivity.class, myBookmarkSearchPatternOption.getValue(), null);
		}
		return true;
	}

	private void gotoBookmark(Bookmark bookmark) {
		bookmark.markAsAccessed();
		myCollection.saveBookmark(bookmark);
		final Book book = myCollection.getBookById(bookmark.BookId);
		if (book != null) {
			FBReader.openBookActivity(this, book, bookmark);
		} else {
			UIMessageUtil.showErrorMessage(this, "cannotOpenBook");
		}
	}

	private final class BookmarksAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
		private final List<Bookmark> myBookmarksList =
			Collections.synchronizedList(new LinkedList<Bookmark>());
		private volatile boolean myShowAddBookmarkItem;

		BookmarksAdapter(ListFragment listFragment, boolean showAddBookmarkItem) {
			myShowAddBookmarkItem = showAddBookmarkItem;
			listFragment.setListAdapter(this);
		}

		public List<Bookmark> bookmarks() {
			return Collections.unmodifiableList(myBookmarksList);
		}

		public void addAll(final List<Bookmark> bookmarks) {
			runOnUiThread(new Runnable() {
				public void run() {
					synchronized (myBookmarksList) {
						for (Bookmark b : bookmarks) {
							final int position = Collections.binarySearch(myBookmarksList, b, myComparator);
							if (position < 0) {
								myBookmarksList.add(- position - 1, b);
							}
						}
					}
					notifyDataSetChanged();
				}
			});
		}

		private boolean areEqualsForView(Bookmark b0, Bookmark b1) {
			return
				b0.getStyleId() == b1.getStyleId() &&
				b0.getText().equals(b1.getText()) &&
				b0.getTimestamp(Bookmark.DateType.Latest).equals(b1.getTimestamp(Bookmark.DateType.Latest));
		}

		public void replace(final Bookmark old, final Bookmark b) {
			if (old != null && areEqualsForView(old, b)) {
				return;
			}
			runOnUiThread(new Runnable() {
				public void run() {
					synchronized (myBookmarksList) {
						if (old != null) {
							myBookmarksList.remove(old);
						}
						final int position = Collections.binarySearch(myBookmarksList, b, myComparator);
						if (position < 0) {
							myBookmarksList.add(- position - 1, b);
						}
					}
					notifyDataSetChanged();
				}
			});
		}

		public void removeAll(final Collection<Bookmark> bookmarks) {
			if (bookmarks.isEmpty()) {
				return;
			}
			runOnUiThread(new Runnable() {
				public void run() {
					myBookmarksList.removeAll(bookmarks);
					notifyDataSetChanged();
				}
			});
		}

		public void clear() {
			runOnUiThread(new Runnable() {
				public void run() {
					myBookmarksList.clear();
					notifyDataSetChanged();
				}
			});
		}

		@Override
		public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
			final Bookmark bookmark = getItem(position);
			if (bookmark == null) {
				return false;
			}

			final ContextMenuDialog dialog = new ContextMenuDialog() {
				@Override
				protected void onItemClick(long itemId) {
					switch ((int)itemId) {
						case OPEN_ITEM_ID:
							gotoBookmark(bookmark);
							break;
						case EDIT_ITEM_ID:
							final Intent intent = new Intent(BookmarksActivity.this, EditBookmarkActivity.class);
							FBReaderIntents.putBookmarkExtra(intent, bookmark);
							OrientationUtil.startActivity(BookmarksActivity.this, intent);
							break;
						case DELETE_ITEM_ID:
							myCollection.deleteBookmark(bookmark);
							break;
					}
				}
			};
			dialog.addItem(OPEN_ITEM_ID, myResource, "openBook");
			dialog.addItem(EDIT_ITEM_ID, myResource, "editBookmark");
			dialog.addItem(DELETE_ITEM_ID, myResource, "deleteBookmark");
			dialog.show(BookmarksActivity.this);
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = (convertView != null) ? convertView :
				LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_item, parent, false);
			final ImageView imageView = ViewUtil.findImageView(view, R.id.bookmark_item_icon);
			final View colorContainer = ViewUtil.findView(view, R.id.bookmark_item_color_container);
			final AmbilWarnaPrefWidgetView colorView =
				(AmbilWarnaPrefWidgetView)ViewUtil.findView(view, R.id.bookmark_item_color);
			final TextView textView = ViewUtil.findTextView(view, R.id.bookmark_item_text);
			final TextView bookTitleView = ViewUtil.findTextView(view, R.id.bookmark_item_booktitle);

			final Bookmark bookmark = getItem(position);
			if (bookmark == null) {
				imageView.setVisibility(View.VISIBLE);
				imageView.setImageResource(R.drawable.ic_list_plus);
				colorContainer.setVisibility(View.GONE);
				textView.setText(myResource.getResource("new").getValue());
				bookTitleView.setVisibility(View.GONE);
			} else {
				imageView.setVisibility(View.GONE);
				colorContainer.setVisibility(View.VISIBLE);
				BookmarksUtil.setupColorView(colorView, myStyles.get(bookmark.getStyleId()));
				textView.setText(bookmark.getText());
				if (myShowAddBookmarkItem) {
					bookTitleView.setVisibility(View.GONE);
				} else {
					bookTitleView.setVisibility(View.VISIBLE);
					bookTitleView.setText(bookmark.BookTitle);
				}
			}
			return view;
		}

		@Override
		public final boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public final boolean isEnabled(int position) {
			return true;
		}

		@Override
		public final long getItemId(int position) {
			final Bookmark item = getItem(position);
			return item != null ? item.getId() : -1;
		}

		@Override
		public final Bookmark getItem(int position) {
			if (myShowAddBookmarkItem) {
				--position;
			}
			return position >= 0 ? myBookmarksList.get(position) : null;
		}

		@Override
		public final int getCount() {
			return myShowAddBookmarkItem ? myBookmarksList.size() + 1 : myBookmarksList.size();
		}

		public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final Bookmark bookmark = getItem(position);
			if (bookmark != null) {
				gotoBookmark(bookmark);
			} else if (myShowAddBookmarkItem) {
				myShowAddBookmarkItem = false;
				myCollection.saveBookmark(myBookmark);
			}
		}
	}

	private static class BookmarksFragment extends ListFragment {
		@Override
		public void onViewCreated(View view, Bundle saved) {
			super.onViewCreated(view, saved);

			final ListView listView = (ListView)view.findViewById(android.R.id.list);
			final BookmarksAdapter adapter = (BookmarksAdapter)getListAdapter();
			listView.setOnItemLongClickListener(adapter);
		}

		@Override
		public void onListItemClick(ListView listView, View view, int position, long id) {
			final BookmarksAdapter adapter = (BookmarksAdapter)getListAdapter();
			adapter.onItemClick(listView, view, position, id);
		}
	}

	// method from IBookCollection.Listener
	public void onBookEvent(BookEvent event, Book book) {
		switch (event) {
			default:
				break;
			case BookmarkStyleChanged:
				runOnUiThread(new Runnable() {
					public void run() {
						updateStyles();
						myAllBooksAdapter.notifyDataSetChanged();
						myThisBookAdapter.notifyDataSetChanged();
						if (mySearchResultsAdapter != null) {
							mySearchResultsAdapter.notifyDataSetChanged();
						}
					}
				});
				break;
			case BookmarksUpdated:
				updateBookmarks(book);
				break;
		}
	}

	// method from IBookCollection.Listener
	public void onBuildEvent(IBookCollection.Status status) {
	}
}
