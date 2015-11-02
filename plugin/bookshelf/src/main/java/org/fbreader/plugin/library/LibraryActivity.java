package org.fbreader.plugin.library;

import java.io.*;
import java.util.*;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.*;
import android.widget.*;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.book.Book;

import org.fbreader.plugin.library.prefs.SettingsActivity;
import org.fbreader.plugin.library.util.*;
import org.fbreader.plugin.library.view.DrawerItem;

public final class LibraryActivity extends FullActivity {
	private static final String PURCHASE_ID_V1 = "remove_ads";
	private static final String PURCHASE_ID_V2 = "remove_ads_v2";
	private static final String PURCHASE_ID_V2_PLUS = "remove_ads_donate_v2";

	private static final int CREATE_SHELF_CODE = 102;
	private static final int SETTINGS_CODE = 103;

	enum GridViewType {
		small_cards(true),
		tiny_cards(true),
		wide_cards(true),
		file_view(false);

		private static final String VIEW_KEY = "fbreader.library.view";

		static GridViewType fromString(String value, GridViewType defaultValue) {
			try {
				return valueOf(value);
			} catch (Exception e) {
				return defaultValue;
			}
		}

		static GridViewType fromPreferences(SharedPreferences prefs, GridViewType defaultValue) {
			return fromString(prefs.getString(VIEW_KEY, null), defaultValue);
		}

		final boolean Selectable;

		GridViewType(boolean selectable) {
			Selectable = selectable;
		}

		void save(SharedPreferences prefs) {
			final SharedPreferences.Editor edit = prefs.edit();
			edit.putString(VIEW_KEY, String.valueOf(this));
			edit.commit();
		}
	}

	private GridViewType myDefaultViewType = GridViewType.small_cards;
	GridViewType SelectedViewType = myDefaultViewType;
	private GridViewType myActualViewType = myDefaultViewType;
	final BookCollectionShadow Collection = new BookCollectionShadow();

	private final DecoratedAdView myAdView = new DecoratedAdView();
	private DrawerLayout myDrawerLayout;
	private ActionBarDrawerToggle myDrawerToggle;
	private final BooksAdapter myBooksAdapter = new BooksAdapter(this);

	private IabHelper myIabHelper;

	private GridView myGrid;
	private DrawerAdapter myDrawerAdapter;
	private ListView myDrawer;
	private volatile View myProgress;

	private final List<String> myCustomCategories =
		Collections.synchronizedList(new ArrayList<String>());

	enum AdState {
		notDefined,
		noGoogleServices,
		active,
		removed
	}

	private volatile AdState myAdState = AdState.notDefined;

	private void setAdState(AdState state) {
		myAdState = state;
		myAdView.init(state);
		supportInvalidateOptionsMenu();
	}

	@Override
	protected int layoutId() {
		return R.layout.bks_library;
	}

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		final Map<String,?> oldSettings = oldPreferences().getAll();
		if (!oldSettings.isEmpty()) {
			final SharedPreferences.Editor old = oldPreferences().edit();
			final SharedPreferences.Editor actual = preferences().edit();
			for (Map.Entry<String,?> s : oldSettings.entrySet()) {
				final String key = s.getKey();
				final Object value = s.getValue();
				if (value instanceof String) {
					actual.putString(key, (String)value);
				} else if (value instanceof Integer) {
					actual.putInt(key, (Integer)value);
				}
				old.remove(key);
			}
			actual.commit();
			old.commit();
		}

		BookUtil.resetPopup();

		myGrid = (GridView)findViewById(R.id.bks_library_grid);

		myDefaultViewType = GridViewType.fromString(
			getResources().getString(R.string.view_mode_default_code),
			myDefaultViewType
		);
		SelectedViewType = GridViewType.fromPreferences(preferences(), myDefaultViewType);

		myIabHelper = new IabHelper(this, BuildConfig.PUBLIC_KEY);
		myIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				switch (result.getResponse()) {
					case IabHelper.BILLING_RESPONSE_RESULT_OK:
						myIabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
							public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
								if (result.isSuccess()) {
									setAdState(
										(inventory.getPurchase(PURCHASE_ID_V1) != null ||
										 inventory.getPurchase(PURCHASE_ID_V2) != null ||
										 inventory.getPurchase(PURCHASE_ID_V2_PLUS) != null)
											? AdState.removed : AdState.active
									);
								}
							}
						});
						break;
					case IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE:
						setAdState(AdState.noGoogleServices);
						break;
					default:
						break;
				}
			}
		});

		myGrid.setAdapter(myBooksAdapter);
		myGrid.setOnScrollListener(myBooksAdapter);
		myDrawerAdapter = new DrawerAdapter();

		myProgress = findViewById(R.id.md_progress_indicator);
		final boolean result = Collection.bindToService(this, new Runnable() {
			@Override
			public void run() {
				Collection.addListener(myBooksAdapter);
				myBooksAdapter.selectSavedCategory();
				myBooksAdapter.invalidateStateCache();
				updateCustomCategoryList(Collection.labels());
				setProgressVisibility(!Collection.status().IsComplete);
			}
		});
		if (!result) {
			setProgressVisibility(false);
			showMissingFBReaderDialog();
		}

		myDrawer = (ListView)findViewById(R.id.bks_library_drawer);
		myDrawer.setAdapter(myDrawerAdapter);
		myDrawer.setOnItemClickListener(myDrawerAdapter);

		myDrawerLayout = ((DrawerLayout)findViewById(R.id.bks_library_drawer_layout));
		myDrawerToggle = new ActionBarDrawerToggle(
			this, myDrawerLayout, getToolbar(), R.string.desc_open_drawer, R.string.desc_close_drawer
		);
		myDrawerLayout.setDrawerListener(myDrawerToggle);
		myDrawerLayout.setDrawerShadow(R.drawable.shadow_right_6dp, GravityCompat.START);
	}

	void setProgressVisibility(boolean visible) {
		if (myProgress != null) {
			myProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedState) {
		super.onPostCreate(savedState);
		myDrawerToggle.syncState();

		resolveVersionConflict();
	}

	private void resolveVersionConflict() {
		final Intent intent = getIntent();
		if (intent == null || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
			return;
		}

		final Intent premiumIntent = new Intent("com.fbreader.action.EXTERNAL_LIBRARY");
		premiumIntent.setPackage("com.fbreader");

		final ResolveInfo info =
			getPackageManager().resolveActivity(premiumIntent, PackageManager.MATCH_DEFAULT_ONLY);
		if (info == null || info.activityInfo == null) {
			return;
		}

		new AlertDialog.Builder(this)
			.setMessage(R.string.conflict_question)
			.setPositiveButton(
				R.string.conflict_premium,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(premiumIntent);
						finish();
					}
				}
			)
			.setNegativeButton(R.string.conflict_bookshelf, null)
			.create()
			.show();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		myDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Collection.bindToService(this, new Runnable() {
			@Override
			public void run() {
				myBooksAdapter.invalidateStateCache();
				updateCustomCategoryList(Collection.labels());
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		myAdView.resume();
	}

	@Override
	protected void onPause() {
		myAdView.pause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		Collection.unbind();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		myAdView.destroy();
		if (myIabHelper != null) {
			myIabHelper.dispose();
			myIabHelper = null;
		}
		super.onDestroy();
	}

	private void selectViewType(GridViewType type) {
		SelectedViewType = type;
		type.save(preferences());
		applyViewType(type);
	}

	private boolean isPortrait() {
		return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}

	private int getNumColumns(String optionKey, int defaultResourceId) {
		return preferences().getInt(optionKey, getResources().getInteger(defaultResourceId));
	}

	void applyViewType(GridViewType type) {
		myActualViewType = type;
		supportInvalidateOptionsMenu();
		setGridSpacing();

		final int numColumns;
		final int stretchModeId;

		switch (type) {
			default:
			case small_cards:
				myGrid.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.small_card_width));
				numColumns = -1;
				stretchModeId = R.integer.small_card_stretch_mode;
				break;
			case tiny_cards:
				myGrid.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.tiny_card_width));
				numColumns = -1;
				stretchModeId = R.integer.tiny_card_stretch_mode;
				break;
			case wide_cards:
				if (isPortrait()) {
					numColumns = getNumColumns(
						"wide_card_column_number_portrait",
						R.integer.wide_card_column_number_portrait
					);
				} else {
					numColumns = getNumColumns(
						"wide_card_column_number_landscape",
						R.integer.wide_card_column_number_landscape
					);
				}
				stretchModeId = R.integer.wide_card_stretch_mode;
				break;
			case file_view:
				if (isPortrait()) {
					numColumns = getNumColumns(
						"file_view_column_number_portrait",
						R.integer.file_view_column_number_portrait
					);
				} else {
					numColumns = getNumColumns(
						"file_view_column_number_landscape",
						R.integer.file_view_column_number_landscape
					);
				}
				stretchModeId = R.integer.file_view_stretch_mode;
				break;
		}

		myGrid.setStretchMode(getResources().getInteger(stretchModeId));
		myGrid.setNumColumns(numColumns);
	}

	private final int[] EXTRA_ITEMS = new int[] {
		R.id.bks_library_menu_all_authors,
		R.id.bks_library_menu_all_series,
		R.id.bks_library_menu_level_up
	};

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.library, menu);

        final MenuItem smallCardsItem = menu.findItem(R.id.bks_library_menu_small_cards);
		smallCardsItem.setChecked(myActualViewType == GridViewType.small_cards);
		smallCardsItem.setEnabled(myActualViewType.Selectable);
        final MenuItem tinyCardsItem = menu.findItem(R.id.bks_library_menu_tiny_cards);
		tinyCardsItem.setChecked(myActualViewType == GridViewType.tiny_cards);
		tinyCardsItem.setEnabled(myActualViewType.Selectable);
        final MenuItem wideCardsItem = menu.findItem(R.id.bks_library_menu_wide_cards);
		wideCardsItem.setChecked(myActualViewType == GridViewType.wide_cards);
		wideCardsItem.setEnabled(myActualViewType.Selectable);

        final MenuItem buyPremiumItem = menu.findItem(R.id.bks_library_menu_buy_premium);
		buyPremiumItem.setVisible(myAdState == AdState.active);

        final MenuItem rescanItem = menu.findItem(R.id.bks_library_menu_rescan);
		rescanItem.setEnabled(Collection.status().IsComplete);

		for (int id : EXTRA_ITEMS) {
			menu.findItem(id).setVisible(myBooksAdapter.extraMenuItemIds().contains(id));
		}

        final MenuItem searchItem = menu.findItem(R.id.bks_library_menu_search);
        final SearchView searchView = (SearchView)MenuItemCompat.getActionView(searchItem);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String query) {
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				final Shelf shelf = new Shelf.SearchShelf(query);
				if (myBooksAdapter.isShelfActive(shelf)) {
					myBooksAdapter.selectShelf(shelf);
					searchView.onActionViewCollapsed();
				} else {
					Toast.makeText(
						LibraryActivity.this, R.string.no_books_found, Toast.LENGTH_SHORT
					).show();
				}
				return false;
			}
		});

        return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.gc();
		System.gc();
		final long itemId = item.getItemId();
		if (itemId == R.id.bks_library_menu_continue_reading) {
			BookUtil.openBook(this, null);
			return true;
		} else if (itemId == R.id.bks_library_menu_small_cards) {
			selectViewType(GridViewType.small_cards);
			return true;
		} else if (itemId == R.id.bks_library_menu_tiny_cards) {
			selectViewType(GridViewType.tiny_cards);
			return true;
		} else if (itemId == R.id.bks_library_menu_wide_cards) {
			selectViewType(GridViewType.wide_cards);
			return true;
		} else if (itemId == R.id.bks_library_menu_old_view) {
			try {
				OrientationUtil.startActivity(this, FBReaderIntents.internalIntent(FBReaderIntents.Action.LIBRARY));
				finish();
			} catch (ActivityNotFoundException e) {
				showMissingFBReaderDialog();
			}
			return true;
		} else if (itemId == R.id.bks_library_menu_all_authors) {
			myBooksAdapter.selectShelf(Shelf.defaultShelf(Shelf.Category.AllAuthors));
			return true;
		} else if (itemId == R.id.bks_library_menu_all_series) {
			myBooksAdapter.selectShelf(Shelf.defaultShelf(Shelf.Category.AllSeries));
			return true;
		} else if (itemId == R.id.bks_library_menu_rescan) {
			if (Collection.status().IsComplete) {
				myBooksAdapter.reset(new Runnable() {
					public void run() {
						Collection.reset(true);
					}
				});
			}
			return true;
		} else if (itemId == R.id.bks_library_menu_settings) {
			OrientationUtil.startActivityForResult(this, new Intent(this, SettingsActivity.class), SETTINGS_CODE);
			return true;
		} else if (itemId == R.id.bks_library_menu_buy_premium) {
			showPremiumDialog();
			return true;
		} else if (itemId == R.id.bks_library_menu_about) {
			String version;
			try {
            	version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (Exception e) {
				version = "unknown";
			}
			showHtmlDialog(
				R.layout.bks_dialog_about,
				fromResourceFile("about").replace("%s", version)
			);
			return true;
		} else if (itemId == R.id.bks_library_menu_whatsnew) {
			showHtmlDialog(R.layout.text_dialog, fromResourceFile("whatsnew"));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private InputStream assetsStream(String name, String locale) {
		try {
			return getResources().getAssets().open(name + "/" + locale + ".html");
		} catch (IOException e) {
			return null;
		}
	}

	private String fromResourceFile(String name) {
		final StringBuffer buffer = new StringBuffer();

		BufferedReader reader = null;
		try {
			final Locale locale = Locale.getDefault();
			InputStream is = assetsStream(name, locale.getLanguage() + "_" + locale.getCountry());
			if (is == null) {
				is = assetsStream(name, locale.getLanguage());
			}
			if (is == null) {
				is = assetsStream(name, "en");
			}
			reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				buffer.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			// ignore
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
			}
		}

		return buffer.toString();
	}

	private void showHtmlDialog(int layoutId, String html) {
		final TextView textView = (TextView)getLayoutInflater().inflate(layoutId, null);
		textView.setText(Html.fromHtml(html));
		textView.setMovementMethod(new LinkMovementMethod());
		new AlertDialog.Builder(this)
			.setView(textView)
			.setPositiveButton(R.string.button_ok, null)
			.create().show();
	}

	private void showPremiumDialog() {
		final TextView textView = (TextView)getLayoutInflater().inflate(R.layout.text_dialog, null);
		textView.setText(Html.fromHtml(fromResourceFile("premium")));
		textView.setMovementMethod(new LinkMovementMethod());
		new AlertDialog.Builder(this)
			.setView(textView)
			.setIcon(0)
			.setPositiveButton(
				R.string.button_buy,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(
							Intent.ACTION_VIEW,
							Uri.parse("market://details?id=com.fbreader")
						));
					}
				}
			)
			.setNegativeButton(R.string.button_no_thanks, null)
			.create().show();
	}

	void showMissingFBReaderDialog() {
		new AlertDialog.Builder(this)
			.setTitle(R.string.app_title)
			.setMessage(R.string.fbreader_not_found)
			.setPositiveButton(R.string.button_install, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("market://details?id=org.geometerplus.zlibrary.ui.android")
					));
				}
			})
			.setNegativeButton(R.string.button_cancel, null)
			.create().show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
			case CREATE_SHELF_CODE:
				if (resultCode == RESULT_OK && data != null) {
					addBookToShelf(
						FBReaderIntents.getBookExtra(data, Collection),
						BookUtil.customCategoryLabel(
							data.getStringExtra(CreateShelfActivity.NEW_SHELF_TITLE_KEY)
						)
					);
				}
				break;
			case SETTINGS_CODE:
				if (!applyTheme()) {
					SelectedViewType =
						GridViewType.fromPreferences(preferences(), myDefaultViewType);
					myDrawerAdapter.resetDefaultShelves();
					Collection.bindToService(this, new Runnable() {
						@Override
						public void run() {
							myBooksAdapter.selectSavedCategory();
							myBooksAdapter.invalidateStateCache();
							updateCustomCategoryList(Collection.labels());
						}
					});
				}
				break;
		}
	}

	private void setGridSpacing() {
		final int padding_id;
		final int spacing_id;
		switch (myActualViewType) {
			default:
				padding_id = R.dimen.grid_padding;
				spacing_id = R.dimen.grid_spacing;
				break;
			case tiny_cards:
			case file_view:
				padding_id = R.dimen.compact_grid_padding;
				spacing_id = R.dimen.compact_grid_spacing;
				break;
		}

		final int normal = getResources().getDimensionPixelSize(padding_id);
		final int bottom = myAdView.IsActive
			? getResources().getDimensionPixelSize(R.dimen.grid_padding_bottom_ad)
			: normal;
		myGrid.setPadding(normal, normal, normal, bottom);

		final int spacing = getResources().getDimensionPixelSize(spacing_id);
		myGrid.setVerticalSpacing(spacing);
		myGrid.setHorizontalSpacing(spacing);
	}

	private class DecoratedAdView {
		volatile boolean IsActive;
		private volatile AdView myAdView;

		synchronized void init(AdState state) {
			IsActive = state == AdState.active || state == AdState.noGoogleServices;

			runOnUiThread(new Runnable() {
				public void run() {
					if (IsActive) {
						adView().setVisibility(View.VISIBLE);
						adView().setEnabled(true);
						adView().loadAd(new AdRequest.Builder().build());
					} else {
						adView().setEnabled(false);
						adView().setVisibility(View.GONE);
					}
					setGridSpacing();
				}
			});
		}

		void resume() {
			if (IsActive) {
				adView().resume();
			} else {
				adView().setEnabled(false);
				adView().setVisibility(View.GONE);
			}
		}

		void pause() {
			if (IsActive) {
				adView().pause();
			}
		}

		void destroy() {
			if (IsActive) {
				adView().destroy();
			}
		}

		private final AdView adView() {
			if (myAdView == null) {
				myAdView = (AdView)findViewById(R.id.bks_library_ad);
			}
			return myAdView;
		}
	}

	@Override
	public void onBackPressed() {
		if (BookUtil.isPopupShown()) {
			BookUtil.dismissPopup();
			return;
		}

		if (!myBooksAdapter.onBackPressed()) {
			super.onBackPressed();
		}
	}

	GridView mainView() {
		return myGrid;
	}

	void invalidateGrid() {
		final GridView grid = myGrid;
		if (grid != null) {
			grid.post(new Runnable() {
				public void run() {
					grid.invalidateViews();
					myBooksAdapter.PositionManager.selectItem();
				}
			});
		}
	}

	void selectItemInternal(final int position) {
		final GridView grid = myGrid;
		if (grid != null) {
			grid.post(new Runnable() {
				public void run() {
					grid.setSelection(position);
				}
			});
		}
	}

	void invalidateDrawer() {
		final ListView drawer = myDrawer;
		if (drawer != null) {
			drawer.post(new Runnable() {
				public void run() {
					drawer.invalidateViews();
				}
			});
		}
	}

	private SharedPreferences oldPreferences() {
		return getSharedPreferences("library", LibraryActivity.MODE_PRIVATE);
	}

	SharedPreferences preferences() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	List<String> customCategoryList() {
		return new ArrayList<String>(myCustomCategories);
	}

	void updateCustomCategoryList(Collection<String> labels) {
		if (labels.isEmpty()) {
			return;
		}

		final Set<String> labelSet = new TreeSet<String>();
		for (String l : labels) {
			if (BookUtil.isCustomCategoryLabel(l) && !myCustomCategories.contains(l)) {
				labelSet.add(l);
			}
		}
		if (labelSet.isEmpty()) {
			return;
		}
		runOnUiThread(new Runnable() {
			public void run() {
				synchronized (myCustomCategories) {
					labelSet.addAll(myCustomCategories);
					myCustomCategories.clear();
					myCustomCategories.addAll(labelSet);
					myDrawerAdapter.notifyDataSetChanged();
					myDrawer.invalidateViews();
				}
			}
		});
	}

	void startShelfCreator(Book book) {
		final Intent intent = new Intent(this, CreateShelfActivity.class);
		FBReaderIntents.putBookExtra(intent, book);
		startActivityForResult(intent, CREATE_SHELF_CODE);
	}

	void addBookToShelf(Book book, String label) {
		if (book == null || label == null) {
			return;
		}

		book.addNewLabel(label);
		Collection.saveBook(book);
		Toast.makeText(
			this,
			getResources().getString(
				R.string.book_added_to_shelf, book.getTitle(), BookUtil.customCategoryTitle(label)
			),
			Toast.LENGTH_SHORT
		).show();
	}

	private final class DrawerAdapter extends BaseAdapter implements ListView.OnItemClickListener {
		private final List<Shelf> myDefaultShelves =
			Collections.synchronizedList(new ArrayList<Shelf>());

		DrawerAdapter() {
			setDefaultShelves();
		}

		private void setDefaultShelves() {
			synchronized (myDefaultShelves) {
				myDefaultShelves.clear();
				myDefaultShelves.addAll(Shelf.visibleDefaultShelves(preferences()));
				myDefaultShelves.addAll(Shelf.fileShelves(preferences()));
			}
		}

		void resetDefaultShelves() {
			runOnUiThread(new Runnable() {
				public void run() {
					setDefaultShelves();
					notifyDataSetChanged();
					myDrawer.invalidateViews();
				}
			});
		}

		@Override
		public int getCount() {
			return myCustomCategories.size() + myDefaultShelves.size();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return myBooksAdapter.isShelfActive(getItem(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Shelf getItem(int position) {
			synchronized (myCustomCategories) {
				final int customCount = myCustomCategories.size();
				if (position < customCount) {
					return new Shelf.CustomShelf(myCustomCategories.get(position));
				} else {
					return myDefaultShelves.get(position - customCount);
				}
			}
		}

		@Override
		public View getView(int position, View view, ViewGroup root) {
			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.bks_drawer_item, null);
			}

			final DrawerItem drawerItem = (DrawerItem)view;
			final Shelf shelf = getItem(position);

			final int textColor = ActivityUtil.getColorFromAttribute(
				LibraryActivity.this,
				isEnabled(position) ? R.attr.mainTextColor : R.attr.disabledTextColor
			);

			final TextView titleView = drawerItem.titleView();
			titleView.setTextColor(textColor);
			titleView.setText(shelf.itemTitle(LibraryActivity.this));

			final TextView summaryView = drawerItem.summaryView();
			summaryView.setTextColor(textColor);
			final String summary = shelf.itemSummary(LibraryActivity.this);
			if (summary != null) {
				summaryView.setVisibility(View.VISIBLE);
				summaryView.setText(summary);
			} else {
				summaryView.setVisibility(View.GONE);
			}

			return view;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			myBooksAdapter.selectShelf(getItem(position));
			myDrawerLayout.closeDrawer(GravityCompat.START);
		}
	}
}
