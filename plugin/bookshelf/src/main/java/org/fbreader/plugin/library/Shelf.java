package org.fbreader.plugin.library;

import java.util.*;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import org.geometerplus.fbreader.book.*;

public abstract class Shelf {
	public enum Category {
		RecentlyOpened(R.string.category_recently_opened),
		RecentlyAdded(R.string.category_recently_added),
		Favorites(R.string.category_favorites),
		AllTitles(R.string.category_titles),
		AllAuthors(R.string.category_authors),
		AllSeries(R.string.category_series),
		Author(-1),
		Series(-1),
		Found(R.string.category_found),
		FileSystem(R.string.category_file_tree),
		Custom(-1);

		final int StringResourceId;

		Category(int stringResourceId) {
			StringResourceId = stringResourceId;
		}
	}

	public static Shelf defaultShelf(Category kind) {
		return ourDefaultShelves.get(kind);
	}

	public static List<Shelf> allDefaultShelves() {
		return new ArrayList<Shelf>(ourDefaultShelves.values());
	}

	public static List<Shelf> fileShelves(SharedPreferences prefs) {
		return Collections.<Shelf>singletonList(new FileSystemShelf(prefs));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static Set<String> visibleShelvesCodes(SharedPreferences prefs, Set<String> allCodes) {
		return prefs.getStringSet(STANDARD_SHELVES_VISIBILITY_KEY, allCodes);
	}

	public static List<Shelf> visibleDefaultShelves(SharedPreferences prefs) {
		final Set<String> allCodes = new TreeSet<String>();
		for (Shelf s : ourDefaultShelves.values()) {
			allCodes.add(s.Kind.toString());
		}
		final Set<String> visibleCodes;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			visibleCodes = visibleShelvesCodes(prefs, allCodes);
		} else {
			visibleCodes = allCodes;
		}

		final List<Shelf> shelves = new ArrayList<Shelf>();
		for (Shelf s : ourDefaultShelves.values()) {
			if (visibleCodes.contains(s.Kind.toString())) {
				shelves.add(s);
			}
		}
		return shelves;
	}

	private static final Map<Category,Shelf> ourDefaultShelves =
		new LinkedHashMap<Category,Shelf>();
	static {
		final Shelf[] defaultShelves = new Shelf[] {
			new FavoritesShelf(),
			new RecentlyAddedShelf(),
			new RecentlyOpenedShelf(),
			new AllTitlesShelf(),
			new AllAuthorsShelf(),
			new AllSeriesShelf()
		};
		for (Shelf s : defaultShelves) {
			ourDefaultShelves.put(s.Kind, s);
		}
	}

	public static final String STANDARD_SHELVES_VISIBILITY_KEY = "visible_standard_shelves";
	public static final String MAX_COUNT_KEY = "max_books_count";

	private static final String NAME_KEY = "fbreader.library.category.name";
	private static final String SUBCATEGORY_KEY = "fbreader.library.category.param";
	private static final String PATH_LIST_KEY = "fbreader.library.category.path.list";

	static Shelf read(SharedPreferences prefs) {
		final Category kind = Category.valueOf(prefs.getString(NAME_KEY, null));
		final String subcategory = prefs.getString(SUBCATEGORY_KEY, null);
		switch (kind) {
			case Custom:
				return new CustomShelf(subcategory);
			case Found:
				return new SearchShelf(subcategory);
			case Author:
				return new AuthorShelf(subcategory);
			case Series:
				return new SeriesShelf(subcategory);
			case FileSystem:
				return new FileSystemShelf(prefs);
			default:
				return ourDefaultShelves.get(kind);
		}
	}

	public final Category Kind;

	protected Shelf(Category kind) {
		Kind = kind;
	}

	public final String getKey(String key) {
		return Kind + ":" + key;
	}	

	protected int getIntValue(SharedPreferences prefs, String key, int defaultValue) {
		return prefs.getInt(getKey(key), defaultValue);
	}

	void save(SharedPreferences prefs) {
		final SharedPreferences.Editor edit = prefs.edit();
		edit.putString(NAME_KEY, String.valueOf(Kind));
		edit.putString(SUBCATEGORY_KEY, subcategory());
		edit.commit();
	}

	abstract boolean isActive(IBookCollection collection, SharedPreferences prefs);

	abstract String windowTitle(LibraryActivity activity);
	abstract String itemTitle(LibraryActivity activity);
	String itemSummary(LibraryActivity activity) {
		return null;
	}

	private String myKeyString;
	final String keyString() {
		if (myKeyString == null) {
			myKeyString = Kind + ":" + state();
		}
		return myKeyString;
	}

	private String mySubcategory;
	final String subcategory() {
		if (mySubcategory == null) {
			mySubcategory = subcategoryInternal();
		}
		return mySubcategory;
	}
	abstract String subcategoryInternal();

	String state() {
		return subcategory();
	}

	private static abstract class DefaultShelf extends Shelf {
		protected DefaultShelf(Category kind) {
			super(kind);
		}

		@Override
		String windowTitle(LibraryActivity activity) {
			return activity.getResources().getString(Kind.StringResourceId);
		}

		@Override
		String itemTitle(LibraryActivity activity) {
			return activity.getResources().getString(Kind.StringResourceId);
		}

		@Override
		String subcategoryInternal() {
			return null;
		}
	}

	private static final class FavoritesShelf extends DefaultShelf {
		private FavoritesShelf() {
			super(Category.Favorites);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return collection.hasBooks(new Filter.ByLabel(Book.FAVORITE_LABEL));
		}
	}

	static final class RecentlyAddedShelf extends DefaultShelf {
		private RecentlyAddedShelf() {
			super(Category.RecentlyAdded);
		}

		int maxBooksCount(SharedPreferences prefs) {
			return getIntValue(prefs, MAX_COUNT_KEY, 24);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return !collection.recentlyAddedBooks(maxBooksCount(prefs)).isEmpty();
		}
	}

	static final class RecentlyOpenedShelf extends DefaultShelf {
		private RecentlyOpenedShelf() {
			super(Category.RecentlyOpened);
		}

		int maxBooksCount(SharedPreferences prefs) {
			return getIntValue(prefs, MAX_COUNT_KEY, 24);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return !collection.recentlyOpenedBooks(maxBooksCount(prefs)).isEmpty();
		}
	}

	private static final class AllTitlesShelf extends DefaultShelf {
		private AllTitlesShelf() {
			super(Category.AllTitles);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return true;
		}
	}

	private static final class AllAuthorsShelf extends DefaultShelf {
		private AllAuthorsShelf() {
			super(Category.AllAuthors);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return true;
		}
	}

	private static final class AllSeriesShelf extends DefaultShelf {
		private AllSeriesShelf() {
			super(Category.AllSeries);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return collection.hasSeries();
		}
	}

	static final class CustomShelf extends Shelf {
		final String Label;

		CustomShelf(String label) {
			super(Category.Custom);
			if (!BookUtil.isCustomCategoryLabel(label)) {
				throw new IllegalArgumentException("Invalid custom label: " + label);
			}
			Label = label;
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return collection.hasBooks(filter());
		}

		@Override
		String windowTitle(LibraryActivity activity) {
			return BookUtil.customCategoryTitle(Label);
		}

		@Override
		String itemTitle(LibraryActivity activity) {
			return BookUtil.customCategoryTitle(Label);
		}

		@Override
		String subcategoryInternal() {
			return Label;
		}

		Filter filter() {
			return new Filter.ByLabel(Label);
		}
	}

	static final class SearchShelf extends Shelf {
		final String Pattern;

		SearchShelf(String pattern) {
			super(Category.Found);
			if (pattern == null) {
				throw new IllegalArgumentException("Invalid pattern (null)");
			}
			Pattern = pattern;
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return !"".equals(Pattern) && collection.hasBooks(filter());
		}

		@Override
		String windowTitle(LibraryActivity activity) {
			return activity.getResources().getString(Kind.StringResourceId, Pattern);
		}

		@Override
		String itemTitle(LibraryActivity activity) {
			return activity.getResources().getString(Kind.StringResourceId, Pattern);
		}

		@Override
		String subcategoryInternal() {
			return Pattern;
		}

		Filter filter() {
			Filter filter = null;
			for (String p : Pattern.trim().split(" ")) {
				if ("".equals(p)) {
					continue;
				}
				if (filter == null) {
					filter = new Filter.ByPattern(p);
				} else {
					filter = new Filter.And(filter, new Filter.ByPattern(p));
				}
			}
			return filter;
		}
	}

	static final class AuthorShelf extends Shelf {
		final Author Author;

		AuthorShelf(Author author) {
			super(Category.Author);
			Author = author;
		}

		private AuthorShelf(String serialized) {
			super(Category.Author);
			final String[] split = serialized.split("\000");
			Author = new Author(split[0], split[1]);
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return true;
		}

		@Override
		String windowTitle(LibraryActivity activity) {
			return Author.DisplayName;
		}

		@Override
		String itemTitle(LibraryActivity activity) {
			return null;
		}

		@Override
		String subcategoryInternal() {
			return Author.DisplayName + "\000" + Author.SortKey;
		}
	}

	static final class SeriesShelf extends Shelf {
		final Series Series;

		SeriesShelf(Series series) {
			super(Category.Series);
			Series = series;
		}

		private SeriesShelf(String series) {
			this(new Series(series));
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return true;
		}

		@Override
		String windowTitle(LibraryActivity activity) {
			return Series.getTitle();
		}

		@Override
		String itemTitle(LibraryActivity activity) {
			return null;
		}

		@Override
		String subcategoryInternal() {
			return Series.getTitle();
		}
	}

	static final class FileSystemShelf extends Shelf {
		private String myPath;

		FileSystemShelf(SharedPreferences prefs) {
			super(Category.FileSystem);

			final String defaultPath;
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				defaultPath = Environment.getExternalStorageDirectory().getPath();
			} else {
				defaultPath = "/";
			}
			myPath = prefs.getString(PATH_LIST_KEY, defaultPath);
			if (myPath == null) {
				myPath = defaultPath != null ? defaultPath : "/";
			}
		}

		String getPath() {
			return myPath;
		}

		void setPath(String path, SharedPreferences prefs) {
			if (path != null) {
				myPath = path;
				final SharedPreferences.Editor editor = prefs.edit();
				editor.putString(PATH_LIST_KEY, path);
				editor.commit();
			}
		}

		@Override
		boolean isActive(IBookCollection collection, SharedPreferences prefs) {
			return true;
		}

		@Override
		String windowTitle(LibraryActivity activity) {
			return myPath;
		}

		@Override
		String itemTitle(LibraryActivity activity) {
			return activity.getResources().getString(Kind.StringResourceId);
		}

		@Override
		String itemSummary(LibraryActivity activity) {
			return myPath;
		}

		@Override
		String subcategoryInternal() {
			return "0";
		}

		@Override
		String state() {
			return myPath;
		}
	}
}
