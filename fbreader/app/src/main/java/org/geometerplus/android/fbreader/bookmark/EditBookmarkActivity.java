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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView;

import org.fbreader.common.android.FBActivity;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.book.*;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

public class EditBookmarkActivity extends FBActivity implements IBookCollection.Listener<Book> {
	private TabLayout myTabLayout;
	private ViewPager myViewPager;

	private final ZLResource myResource = ZLResource.resource("editBookmark");

	private Fragment myTextFragment = new TextFragment();
	private Fragment myStylesFragment = new StylesFragment();
	private Fragment myDeleteFragment = new DeleteFragment();

	private final BookCollectionShadow myCollection = new BookCollectionShadow();
	private Bookmark myBookmark;
	private StyleListAdapter myStylesAdapter;

	@Override
	protected int layoutId() {
		return R.layout.edit_bookmark;
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setTitle("");

        myTabLayout = (TabLayout)findViewById(R.id.edit_bookmark_tab_layout);
        myViewPager = (ViewPager)findViewById(R.id.edit_bookmark_view_pager);

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
						return myTextFragment;
					case 1:
						return myStylesFragment;
					case 2:
						return myDeleteFragment;
				}
			}

			@Override
			public CharSequence getPageTitle(int position) {
				final String key;
				switch (position) {
					default:
					case 0:
						key = "text";
						break;
					case 1:
						key = "style";
						break;
					case 2:
						key = "delete";
						break;
				}
				return myResource.getResource(key).getValue();
			}
		};
		myViewPager.setAdapter(adapter);
		myTabLayout.setupWithViewPager(myViewPager);

		myBookmark = FBReaderIntents.getBookmarkExtra(getIntent());
		if (myBookmark == null) {
			finish();
			return;
		}

		setWindowSize();

		final ZLIntegerOption currentTabOption =
			new ZLIntegerOption("LookNFeel", "EditBookmarkTab", 0);
		myViewPager.setCurrentItem(currentTabOption.getValue());
		myTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				myViewPager.setCurrentItem(tab.getPosition(), false);
				if (tab.getPosition() != 2 /* delete tab */) {
					currentTabOption.setValue(tab.getPosition());
				}
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
			}
		});
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setWindowSize();
	}

	private void setWindowSize() {
		final DisplayMetrics dm = getResources().getDisplayMetrics();
		final int width = Math.min(
			(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, dm),
			dm.widthPixels * 9 / 10
		);
		final int height = Math.min(
			(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, dm),
			dm.heightPixels * 9 / 10
		);

        final LinearLayout root = (LinearLayout)findViewById(R.id.edit_bookmark);
		root.setLayoutParams(new FrameLayout.LayoutParams(
			new ViewGroup.LayoutParams(width, height)
		));
	}

	@Override
	protected void onDestroy() {
		myCollection.unbind();
		super.onDestroy();
	}

	// method from IBookCollection.Listener
	public void onBookEvent(BookEvent event, Book book) {
		if (event == BookEvent.BookmarkStyleChanged) {
			myStylesAdapter.setStyleList(myCollection.highlightingStyles());
		}
	}

	// method from IBookCollection.Listener
	public void onBuildEvent(IBookCollection.Status status) {
	}

	private class StyleListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
		private final List<HighlightingStyle> myStyles;

		StyleListAdapter(List<HighlightingStyle> styles) {
			myStyles = new ArrayList<HighlightingStyle>(styles);
		}

		public synchronized void setStyleList(List<HighlightingStyle> styles) {
			myStyles.clear();
			myStyles.addAll(styles);
			notifyDataSetChanged();
		}

		public final synchronized int getCount() {
			return myStyles.size();
		}

		public final synchronized HighlightingStyle getItem(int position) {
			return myStyles.get(position);
		}

		public final long getItemId(int position) {
			return position;
		}

		public final synchronized View getView(int position, View convertView, final ViewGroup parent) {
			final View view = convertView != null
				? convertView
				: LayoutInflater.from(parent.getContext()).inflate(R.layout.style_item, parent, false);
			final HighlightingStyle style = getItem(position);

			final CheckBox checkBox = (CheckBox)ViewUtil.findView(view, R.id.style_item_checkbox);
			final AmbilWarnaPrefWidgetView colorView =
				(AmbilWarnaPrefWidgetView)ViewUtil.findView(view, R.id.style_item_color);
			final TextView titleView = ViewUtil.findTextView(view, R.id.style_item_title);
			final Button button = (Button)ViewUtil.findView(view, R.id.style_item_edit_button);

			checkBox.setChecked(style.Id == myBookmark.getStyleId());

			colorView.setVisibility(View.VISIBLE);
			BookmarksUtil.setupColorView(colorView, style);

			titleView.setText(BookmarkUtil.getStyleName(style));

			button.setVisibility(View.VISIBLE);
			button.setText(myResource.getResource("editStyle").getValue());
			button.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View view) {
					startActivity(
						new Intent(EditBookmarkActivity.this, EditStyleActivity.class)
							.putExtra(EditStyleActivity.STYLE_ID_KEY, style.Id)
					);
				}
			});

			return view;
		}

		public final synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final HighlightingStyle style = getItem(position);
			myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
				public void run() {
					myBookmark.setStyleId(style.Id);
					myCollection.setDefaultHighlightingStyleId(style.Id);
					myCollection.saveBookmark(myBookmark);
				}
			});
			notifyDataSetChanged();
		}
	}

	private class TextFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
			if (container == null) {
				return null;
			}

			final View view = inflater.inflate(R.layout.edit_bookmark_text, container, false);

			final EditText editor = (EditText)view.findViewById(R.id.edit_bookmark_text);
			editor.setText(myBookmark.getText());
			final int len = editor.getText().length();
			editor.setSelection(len, len);

			final Button saveTextButton = (Button)view.findViewById(R.id.edit_bookmark_save_text_button);
			saveTextButton.setEnabled(false);
			saveTextButton.setText(myResource.getResource("saveText").getValue());
			saveTextButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
						public void run() {
							myBookmark.setText(editor.getText().toString());
							myCollection.saveBookmark(myBookmark);
							saveTextButton.setEnabled(false);
						}
					});
				}
			});
			editor.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence sequence, int start, int before, int count) {
					final String originalText = myBookmark.getText();
					saveTextButton.setEnabled(!originalText.equals(editor.getText().toString()));
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});

			return view;
		}
	}

	private class StylesFragment extends ListFragment {
		@Override
		public void onViewCreated(View view, Bundle saved) {
			super.onViewCreated(view, saved);

			myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
				public void run() {
					final List<HighlightingStyle> styles = myCollection.highlightingStyles();
					if (styles.isEmpty()) {
						finish();
						return;
					}
					myStylesAdapter = new StyleListAdapter(styles);
					setListAdapter(myStylesAdapter);
					myCollection.addListener(EditBookmarkActivity.this);
				}
			});
		}

		@Override
		public void onListItemClick(ListView listView, View view, int position, long id) {
			final StyleListAdapter adapter = (StyleListAdapter)getListAdapter();
			adapter.onItemClick(listView, view, position, id);
		}
	}

	private class DeleteFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
			if (container == null) {
				return null;
			}
			final View view = inflater.inflate(R.layout.edit_bookmark_delete, container, false);
			final Button deleteButton = (Button)view.findViewById(R.id.edit_bookmark_delete_button);
			deleteButton.setText(myResource.getResource("deleteBookmark").getValue());
			deleteButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
						public void run() {
							myCollection.deleteBookmark(myBookmark);
							finish();
						}
					});
				}
			});
			return view;
		}
	}
}
