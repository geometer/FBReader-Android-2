package org.fbreader.plugin.library;

import java.io.File;
import java.util.*;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.fbreader.util.NaturalOrderComparator;
import org.fbreader.util.Pair;
import org.fbreader.util.android.LinearIndexer;

import org.geometerplus.zlibrary.core.util.RationalNumber;
import org.geometerplus.fbreader.book.*;
import org.geometerplus.fbreader.book.Filter;

import org.fbreader.plugin.library.view.*;

final class BooksAdapter extends BaseAdapter implements IBookCollection.Listener<Book>, AbsListView.OnScrollListener, SectionIndexer {
	private static final NaturalOrderComparator NATURAL_ORDER_COMPARATOR =
		new NaturalOrderComparator();

	interface ItemViewType {
		int SmallCards = 0;
		int TinyCards = 3;
		int WideCards = 6;

		int Book = 0;
		int Author = 1;
		int Series = 2;

		int File = 9;
	}

	private interface SetupAction {
		SetupAction run();
	}

	private static final Comparator<Book> TITLE_COMPARATOR = new Comparator<Book>() {
		@Override
		public int compare(Book b0, Book b1) {
			return b0.compareTo(b1);
		}
	};

	private static final Comparator<Book> SERIES_INDEX_COMPARATOR = new Comparator<Book>() {
		@Override
		public int compare(Book b0, Book b1) {
			return b0.getSeriesInfo().compareTo(b1.getSeriesInfo());
		}
	};

	private abstract class Provider {
		abstract SetupAction setupAction();
		abstract SectionIndexer sectionIndexer();
		abstract boolean onBookEventList(List<Pair<BookEvent,Book>> events);

		void reset() {
			synchronized (myItemList) {
				myItemList.clear();
			}
		}

		SparseArray<BookActionMenu.Action> extraActions() {
			return SparseArrayUtil.empty();
		}

		LibraryActivity.GridViewType viewType() {
			return myActivity.SelectedViewType;
		}

		void onShelfReselecting() {
		}

		boolean onBackPressed() {
			return false;
		}

		Set<Integer> extraMenuItemIds() {
			return Collections.emptySet();
		}

		String bookTitle(Book book) {
			return book.getTitle();
		}
	}

	private final LibraryActivity myActivity;
	private final List myItemList = new ArrayList();
	private Provider myProvider;
	private final Map<Shelf,Boolean> myCategoryStateCache =
		Collections.synchronizedMap(new HashMap<Shelf,Boolean>());

	BooksAdapter(LibraryActivity activity) {
		myActivity = activity;
	}

	private Filter defaultFilter() {
		return myActivity.preferences().getBoolean("show_intro", false)
			? new Filter.Empty() : new Filter.HasPhysicalFile();
	}

	private void addBooksToList(Collection<Book> books) {
		final Filter filter = defaultFilter();
		for (Book b : books) {
			if (filter.matches(b)) {
				myItemList.add(b);
			}
		}
	}

	@Override
	public int getViewTypeCount() {
		return 3 /* book, author, series */ * 3 /* wide, small, tiny */ + 1 /* file */;
	}

	@Override
	public int getCount() {
		return myItemList.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return myItemList.get(position);
		} catch (Exception e) {
			return null;
		}
	}

	public int getItemViewType(int position) {
		final Object item = getItem(position);
		if (item instanceof FileItem) {
			return ItemViewType.File;
		}

		final int vt;
		switch (myActivity.SelectedViewType) {
			default:
			case small_cards:
				vt = ItemViewType.SmallCards;
				break;
			case tiny_cards:
				vt = ItemViewType.TinyCards;
				break;
			case wide_cards:
				vt = ItemViewType.WideCards;
				break;
		}

		if (item instanceof Book) {
			return vt + ItemViewType.Book;
		} else if (item instanceof Author) {
			return vt + ItemViewType.Author;
		} else /* series item */ {
			return vt + ItemViewType.Series;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	private int favResource(boolean favorite) {
		return favorite
			? android.R.drawable.btn_star_big_on
			: android.R.drawable.btn_star_big_off;
	}

	private void setIcon(ImageView imageView, int imageId) {
		BookUtil.setImageResource(imageView, imageId);
	}

	private void setIcon(ImageView imageView, int smallCardImageId, int wideCardImageId) {
		switch (myActivity.SelectedViewType) {
			case tiny_cards:
			case small_cards:
				BookUtil.setImageResource(imageView, smallCardImageId);
			default:
				BookUtil.setImageResource(imageView, wideCardImageId);
		}
	}

	private void setBookIcon(ImageView imageView) {
		setIcon(imageView, R.drawable.fbreader_small_card, R.drawable.fbreader_wide_card);
	}

	private void setupBookView(final BookView view, final Book book) {
		if (view.getTag() == book) {
			return;
		}

		view.setTag(book);

		final ImageView coverView = view.coverView();
		final Bitmap cover = BookUtil.getCover(book);
		if (cover != null) {
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
					synchronized (view) {
						if (view.getTag() == book) {
							coverView.setImageBitmap(cover);
						}
					}
				}
			});
		} else {
			setBookIcon(coverView);
			BookUtil.retrieveCover(myActivity, book, new BookUtil.BitmapRunnable() {
				public void run(final Bitmap bmp) {
					if (bmp == null || view.getTag() != book) {
						return;
					}

					myActivity.runOnUiThread(new Runnable() {
						public void run() {
							synchronized (view) {
								if (view.getTag() == book) {
									coverView.setImageBitmap(bmp);
								}
							}
						}
					});
				}
			});
		}

		final TextView authorsView = view.authorsView();
		if (authorsView != null) {
			final List<Author> authors = book.authors();
			if (!authors.isEmpty()) {
				final StringBuilder text = new StringBuilder();
				text.append(authors.get(0).DisplayName);
				for (int i = 1; i < 3; ++i) {
					if (authors.size() > i) {
						text.append(", ");
						text.append(authors.get(i).DisplayName);
					} else {
						break;
					}
				}
				authorsView.setVisibility(View.VISIBLE);
				authorsView.setText(text.toString());
			} else {
				authorsView.setVisibility(View.GONE);
			}
		}

		final TextView titleView = view.titleView();
		if (titleView != null) {
			final String title = myProvider.bookTitle(book);
			if (title.length() > 0) {
				titleView.setVisibility(View.VISIBLE);
				titleView.setText(title);
			} else {
				titleView.setVisibility(View.GONE);
			}
		}

		final TextView statusView = view.statusView();
		if (statusView != null) {
			final RationalNumber progress = book.getProgress();
			if (progress != null) {
				statusView.setVisibility(View.VISIBLE);
				statusView.setText(myActivity.getResources().getString(
					view.progressTextId(),
					100 * progress.Numerator / progress.Denominator
				));
			} else {
				statusView.setVisibility(View.GONE);
			}
		}

		final View favContainerView = view.favContainerView();
		if (favContainerView != null) {
			final View favView = view.favView();
			favView.setBackgroundResource(favResource(book.hasLabel(Book.FAVORITE_LABEL)));
			favContainerView.setOnClickListener(new BookUtil.OnClickListener() {
				@Override
				protected void onClick2() {
					final boolean favorite = book.hasLabel(Book.FAVORITE_LABEL);
					if (favorite) {
						book.removeLabel(Book.FAVORITE_LABEL);
					} else {
						book.addNewLabel(Book.FAVORITE_LABEL);
					}
					favView.setBackgroundResource(favResource(!favorite));
					myActivity.Collection.saveBook(book);
				}
			});
		}

		view.setOnClickListener(new BookUtil.OnClickListener() {
			@Override
			protected void onClick2() {
				BookUtil.showPopup(myActivity, book, myProvider.extraActions());
			}
		});
		final View readButton = view.readButton();
		if (readButton != null) {
			readButton.setOnClickListener(new BookUtil.OnClickListener() {
				@Override
				protected void onClick2() {
					BookUtil.openBook(myActivity, book);
				}
			});
		}
		final View moreButton = view.moreButton();
		if (moreButton != null) {
			moreButton.setOnClickListener(new BookUtil.OnClickListener() {
				@Override
				protected void onClick2() {
					new BookActionMenu(
						myActivity, null, book, moreButton, R.menu.more, myProvider.extraActions()
					).show();
				}
			});
		}
	}

	private View.OnClickListener chdirListener(final FileItem item) {
		return new BookUtil.OnClickListener() {
			@Override
			protected void onClick2() {
				myProvider.onShelfReselecting();
				item.chdir(myActivity.preferences());
				reselectShelf(item.Shelf);
				myActivity.invalidateDrawer();
			}
		};
	}

	private void setupFileView(final FileView view, final FileItem item) {
		if (view.getTag() == item) {
			return;
		}

		view.setTag(item);

		view.nameView().setText(item.Name);
		final ImageView iconView = view.iconView();
		switch (item.Kind) {
			case Parent:
				setIcon(iconView, R.drawable.folder_up);
				view.setEnabled(true);
				view.setOnClickListener(chdirListener(item));
				break;
			case Folder:
				if (item.File.canRead() && item.File.canExecute()) {
					setIcon(iconView, R.drawable.folder);
					view.setEnabled(true);
					view.setOnClickListener(chdirListener(item));
				} else {
					setIcon(iconView, R.drawable.folder_denied);
					view.setEnabled(false);
				}
				break;
			case ZipArchive:
				setIcon(iconView, R.drawable.folder_archive);
				view.setEnabled(true);
				view.setOnClickListener(chdirListener(item));
				break;
			case ZipEntry:
			case File:
			{
				final Book book = item.getBook(myActivity.Collection);
				if (book != null) {
					setIcon(iconView, R.drawable.fbreader_wide_card);
					BookUtil.retrieveCover(myActivity, book, new BookUtil.BitmapRunnable() {
						public void run(final Bitmap bmp) {
							if (bmp == null || view.getTag() != item) {
								return;
							}

							myActivity.runOnUiThread(new Runnable() {
								public void run() {
									synchronized (view) {
										if (view.getTag() == item) {
											iconView.setImageBitmap(bmp);
										}
									}
								}
							});
						}
					});
					view.setEnabled(true);
					view.setOnClickListener(new BookUtil.OnClickListener() {
						@Override
						protected void onClick2() {
							BookUtil.showPopup(myActivity, book, myProvider.extraActions());
						}
					});
				} else {
					setIcon(iconView, R.drawable.file);
					view.setEnabled(false);
				}
				break;
			}
		}
	}

	private class SeriesCoversHolder {
		private final SeriesView myView;
		private final String mySeries;
		private final Bitmap[] myCovers;
		private volatile int myLeft;

		SeriesCoversHolder(SeriesView view, String series, int len) {
			myView = view;
			mySeries = series;
			myCovers = new Bitmap[len];
			myLeft = len;
		}

		private void setCoverInternal() {
			if (myView.getTag() != mySeries) {
				return;
			}
			final Bitmap stacked = BookUtil.stack(myCovers);
			if (stacked == null) {
				return;
			}
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
					synchronized (myView) {
						if (myView.getTag() == mySeries) {
							myView.coversView().setImageBitmap(stacked);
						}
					}
				}
			});
		}

		synchronized void setCover(int index, Bitmap bmp) {
			myCovers[index] = bmp;
			--myLeft;
			if (myLeft == 0) {
				setCoverInternal();
			}
		}
	}

	private void setupSeriesView(SeriesView view, final String series) {
		if (view.getTag() == series) {
			return;
		}

		view.setTag(series);

		view.titleView().setText(series);

		final ImageView coversView = view.coversView();
		setBookIcon(coversView);
		final List<Book> books =
			myActivity.Collection.books(new BookQuery(new Filter.BySeries(new Series(series)), 3));
		final SeriesCoversHolder holder = new SeriesCoversHolder(view, series, books.size());
		for (int i = 0; i < books.size(); ++i) {
			final int index = i;
			BookUtil.retrieveCover(myActivity, books.get(i), new BookUtil.BitmapRunnable() {
				public void run(Bitmap bmp) {
					holder.setCover(index, bmp);
				}
			});
		}

		view.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				selectShelf(new Shelf.SeriesShelf(series));
			}
		});
	}

	private int getViewResourceId(int position) {
		switch (getItemViewType(position)) {
			default:
			case ItemViewType.SmallCards + ItemViewType.Book:
				return R.layout.bks_small_card_book;
			case ItemViewType.SmallCards + ItemViewType.Author:
				return R.layout.bks_small_card_author;
			case ItemViewType.SmallCards + ItemViewType.Series:
				return R.layout.bks_small_card_series;
			case ItemViewType.TinyCards + ItemViewType.Book:
				return R.layout.bks_tiny_card_book;
			case ItemViewType.TinyCards + ItemViewType.Author:
				return R.layout.bks_tiny_card_author;
			case ItemViewType.TinyCards + ItemViewType.Series:
				return R.layout.bks_tiny_card_series;
			case ItemViewType.WideCards + ItemViewType.Book:
				return R.layout.bks_wide_card_book;
			case ItemViewType.WideCards + ItemViewType.Author:
				return R.layout.bks_wide_card_author;
			case ItemViewType.WideCards + ItemViewType.Series:
				return R.layout.bks_wide_card_series;
			case ItemViewType.File:
				return R.layout.bks_compact_card_file;
		}
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			view = myActivity.getLayoutInflater().inflate(getViewResourceId(position), null);
		}

		final Object item = getItem(position);
		if (item instanceof Book) {
			synchronized (view) {
				setupBookView((BookView)view, (Book)item);
			}
		} else if (item instanceof FileItem) {
			synchronized (view) {
				setupFileView((FileView)view, (FileItem)item);
			}
		} else if (item instanceof Author) {
			final Author author = (Author)item;
			((AuthorView)view).titleView().setText(author.DisplayName);
			view.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					selectShelf(new Shelf.AuthorShelf(author));
				}
			});
		} else if (item instanceof String) {
			synchronized (view) {
				setupSeriesView((SeriesView)view, (String)item);
			}
		}
		return view;
	}

	private abstract class BooksProvider extends Provider {
		@Override
		boolean onBookEventList(List<Pair<BookEvent,Book>> events) {
			boolean updated = false;
			for (Pair<BookEvent,Book> pair : events) {
				if (processBookEvent(pair.First, pair.Second)) {
					updated = true;
				}
			}
			return updated;
		}

		boolean processBookEvent(BookEvent event, final Book book) {
			if (!myItemList.contains(book)) {
				return false;
			}

			switch (event) {
				default:
					return false;
				case Updated:
					synchronized (myItemList) {
						final int index = myItemList.indexOf(book);
						if (index != -1) {
							((Book)myItemList.get(index)).updateFrom(book);
							return true;
						}
						return false;
					}
				case Removed:
					synchronized (myItemList) {
						return myItemList.remove(book);
					}
			}
		}
	}

	private abstract class SimpleBooksProvider<S extends Shelf> extends BooksProvider {
		protected final S myShelf;

		SimpleBooksProvider(S shelf) {
			myShelf = shelf;
		}

		protected void setBooks(final Collection<Book> books) {
			synchronized (myItemList) {
				myItemList.clear();
				addBooksToList(books);
			}
		}
	}

	private final class RecentlyAddedProvider extends SimpleBooksProvider<Shelf.RecentlyAddedShelf> {
		RecentlyAddedProvider(Shelf.RecentlyAddedShelf shelf) {
			super(shelf);
		}

		@Override
		SectionIndexer sectionIndexer() {
			return null;
		}

		@Override
		public SetupAction setupAction() {
			return new SetupAction() {
				public SetupAction run() {
					final int limit = myShelf.maxBooksCount(myActivity.preferences());
					setBooks(myActivity.Collection.recentlyAddedBooks(limit));
					notifyDataSetChanged();
					myActivity.invalidateGrid();
					return null;
				}
			};
		}

		@Override
		boolean onBookEventList(List<Pair<BookEvent,Book>> events) {
			final int limit = myShelf.maxBooksCount(myActivity.preferences());
			final List<Book> books = myActivity.Collection.recentlyAddedBooks(limit);
			if (!books.equals(myItemList)) {
				setBooks(books);
				return true;
			}

			return super.onBookEventList(events);
		}
	}

	private final class RecentlyOpenedProvider extends SimpleBooksProvider<Shelf.RecentlyOpenedShelf> {
		RecentlyOpenedProvider(Shelf.RecentlyOpenedShelf shelf) {
			super(shelf);
		}

		@Override
		SectionIndexer sectionIndexer() {
			return null;
		}

		@Override
		public SetupAction setupAction() {
			return new SetupAction() {
				public SetupAction run() {
					final int limit = myShelf.maxBooksCount(myActivity.preferences());
					setBooks(myActivity.Collection.recentlyOpenedBooks(limit));
					notifyDataSetChanged();
					myActivity.invalidateGrid();
					return null;
				}
			};
		}

		@Override
		boolean onBookEventList(List<Pair<BookEvent,Book>> events) {
			final int limit = myShelf.maxBooksCount(myActivity.preferences());
			final List<Book> books = myActivity.Collection.recentlyOpenedBooks(limit);
			if (!books.equals(myItemList)) {
				setBooks(books);
				return true;
			}

			return super.onBookEventList(events);
		}

		@Override
		SparseArray<BookActionMenu.Action> extraActions() {
			return SparseArrayUtil.<BookActionMenu.Action>singleton(
				R.id.bks_book_action_remove_from_recent,
				new BookActionMenu.Action() {
					@Override
					void run(LibraryActivity activity, Book book) {
						activity.Collection.removeFromRecentlyOpened(book);
					}

					@Override
					boolean closePopup() {
						return true;
					}
				}
			);
		}
	}

	private class FilterProvider extends BooksProvider {
		private final Filter myFilter;
		private final Comparator<Book> myComparator;

		FilterProvider(Filter filter, Comparator<Book> comparator) {
			myFilter = filter;
			myComparator = comparator;
		}

		@Override
		SectionIndexer sectionIndexer() {
			// TODO: indexer
			return null;
		}

		@Override
		public SetupAction setupAction() {
			return new SetupAction() {
				public SetupAction run() {
					addBooks(Collections.<Book>emptyList(), true);
					return setupAction(new BookQuery(myFilter, 20));
				}
			};
		}

		private SetupAction setupAction(final BookQuery query) {
			return new SetupAction() {
				public SetupAction run() {
					final List<Book> books = myActivity.Collection.books(query);
					if (books.isEmpty()) {
						return null;
					}
					addBooks(books, false);
					return setupAction(query.next());
				}
			};
		}

		@Override
		boolean processBookEvent(BookEvent event, Book book) {
			switch (event) {
				default:
					return super.processBookEvent(event, book);
				case Added:
					return myFilter.matches(book) && addSingleBook(book, myComparator);
				case Updated:
					if (myFilter.matches(book)) {
						if (!myItemList.contains(book)) {
							return addSingleBook(book, myComparator);
						}
					} else if (myItemList.contains(book)) {
						synchronized (myItemList) {
							return myItemList.remove(book);
						}
					}
					return super.processBookEvent(event, book);
			}
		}

		private boolean addSingleBook(final Book book, final Comparator<Book> comparator) {
			synchronized (myItemList) {
				if (myItemList.contains(book)) {
					return false;
				}
				if (!defaultFilter().matches(book)) {
					return false;
				}

				if (comparator != null) {
					final int index = Collections.binarySearch(
						(List<Book>)myItemList, book, comparator
					);
					myItemList.add(index < 0 ? - index - 1 : index, book);
				} else {
					myItemList.add(book);
				}
			}
			return true;
		}

		private void addBooks(final Collection<Book> books, final boolean removeOld) {
			final Set<Book> newBooks = new HashSet<Book>(books);
			synchronized (myItemList) {
				if (!removeOld) {
					newBooks.addAll(myItemList);
				}
				myItemList.clear();
				addBooksToList(newBooks);
				if (myComparator != null) {
					Collections.sort((List<Book>)myItemList, myComparator);
				}
			}
			notifyDataSetChanged();
			myActivity.invalidateGrid();
		}
	}

	private class SingleAuthorProvider extends FilterProvider {
		SingleAuthorProvider(Shelf.AuthorShelf shelf) {
			super(new Filter.ByAuthor(shelf.Author), TITLE_COMPARATOR);
		}

		@Override
		boolean onBackPressed() {
			selectShelf(Shelf.defaultShelf(Shelf.Category.AllAuthors));
			return true;
		}

		@Override
		Set<Integer> extraMenuItemIds() {
			return Collections.singleton(R.id.bks_library_menu_all_authors);
		}
	}

	private class SingleSeriesProvider extends FilterProvider {
		SingleSeriesProvider(Shelf.SeriesShelf shelf) {
			super(new Filter.BySeries(new Series(shelf.Series)), SERIES_INDEX_COMPARATOR);
		}

		@Override
		boolean onBackPressed() {
			selectShelf(Shelf.defaultShelf(Shelf.Category.AllSeries));
			return true;
		}

		@Override
		Set<Integer> extraMenuItemIds() {
			return Collections.singleton(R.id.bks_library_menu_all_series);
		}

		@Override
		String bookTitle(Book book) {
			final SeriesInfo info = book.getSeriesInfo();
			if (info == null || info.Index == null) {
				return book.getTitle();
			} else {
				return "#" + info.Index.toPlainString() + " " + book.getTitle();
			}
		}
	}

	private static class FileItem implements Comparable<FileItem> {
		private enum FileKind {
			Parent,
			Folder,
			ZipArchive,
			ZipEntry,
			File;
		}

		private final Shelf.FileSystemShelf Shelf;
		private final File File;
		private final String Path;
		private final String Name;
		private final FileKind Kind;

		private static final Object NULL_BOOK = new Object();
		private Object myBook;

		FileItem(Shelf.FileSystemShelf shelf, File file) {
			Shelf = shelf;
			File = file;
			Path = null;
			Name = file.getName();
			if ("..".equals(Name)) {
				Kind = FileKind.Parent;
			} else if (file.isDirectory()) {
				Kind = FileKind.Folder;
			} else if (Name != null && Name.endsWith(".zip") && !Name.endsWith(".fb2.zip")) {
				Kind = FileKind.ZipArchive;
			} else {
				Kind = FileKind.File;
			}
		}

		FileItem(Shelf.FileSystemShelf shelf, File archive, String entryName) {
			Shelf = shelf;
			File = archive;
			Path = entryName;
			Name = entryName.substring(entryName.lastIndexOf("/") + 1);
			Kind = FileKind.ZipEntry;
		}

		void chdir(SharedPreferences prefs) {
			switch (Kind) {
				case Parent:
					Shelf.setPath(new File(Shelf.getPath()).getParent(), prefs);
					break;
				case Folder:
				case ZipArchive:
					Shelf.setPath(File.getPath(), prefs);
					break;
				case ZipEntry:
				case File:
					break;
			}
		}

		synchronized Book getBook(IBookCollection<Book> collection) {
			if (myBook == null) {
				final String path = Path != null ? File.getPath() + ":" + Path : File.getPath();
				final Book book = collection.getBookByFile(path);
				myBook = book != null ? book : NULL_BOOK;
			}
			return myBook == NULL_BOOK ? null : (Book)myBook;
		}

		public int compareTo(FileItem other) {
			if (Kind != other.Kind) {
				return Kind.compareTo(other.Kind);
			}
			return NATURAL_ORDER_COMPARATOR.compare(Name, other.Name);
		}
	}

	private static String label(String title) {
		if (title == null || "".equals(title)) {
			return "";
		} else if (Character.isDigit(title.charAt(0))) {
			return "0-9";
		} else {
			return title.substring(0, 1).toUpperCase();
		}
	}

	private final class FileSystemProvider extends Provider {
		private Shelf.FileSystemShelf myShelf;
		private final List<String> myHistory =
			Collections.synchronizedList(new LinkedList<String>());
		private final LinearIndexer myIndexer = new LinearIndexer();

		FileSystemProvider(Shelf.FileSystemShelf shelf) {
			myShelf = shelf;
		}

		@Override
		SectionIndexer sectionIndexer() {
			return myIndexer;
		}

		@Override
		SetupAction setupAction() {
			return new SetupAction() {
				public SetupAction run() {
					final List<FileItem> itemList = new ArrayList<FileItem>();
					final String path = myShelf.getPath();
					if (!"/".equals(path)) {
						itemList.add(new FileItem(myShelf, new File("..")));
					}
					final FileItem currentItem = new FileItem(myShelf, new File(path));
					switch (currentItem.Kind) {
						default:
							break;
						case Folder:
						{
							final File[] children = currentItem.File.listFiles();
							if (children != null) {
								final TreeSet<FileItem> items = new TreeSet<FileItem>();
								for (File f : children) {
									if (!f.getName().startsWith(".")) {
										items.add(new FileItem(myShelf, f));
									}
								}
								itemList.addAll(items);
							}
							break;
						}
						case ZipArchive:
						{
							final TreeSet<FileItem> items = new TreeSet<FileItem>();
							for (String name : ZipUtil.entries(currentItem.File)) {
								items.add(new FileItem(myShelf, currentItem.File, name));
							}
							itemList.addAll(items);
							break;
						}
					}

					synchronized (myItemList) {
						myItemList.clear();
						myIndexer.reset();

						myItemList.addAll(itemList);

						for (int i = 0; i < itemList.size(); ++i) {
							myIndexer.addElement(label(itemList.get(i).Name));
						}
					}

					notifyDataSetChanged();
					myActivity.invalidateGrid();
					return null;
				}
			};
		}

		@Override
		boolean onBookEventList(List<Pair<BookEvent,Book>> events) {
			return false;
		}

		@Override
		void reset() {
			setupAction().run();
		}

		@Override
		LibraryActivity.GridViewType viewType() {
			return LibraryActivity.GridViewType.file_view;
		}

		@Override
		void onShelfReselecting() {
			myHistory.add(myShelf.getPath());
		}

		@Override
		boolean onBackPressed() {
			synchronized (myHistory) {
				if (myHistory.isEmpty()) {
					return false;
				}
				myShelf.setPath(myHistory.remove(myHistory.size() - 1), myActivity.preferences());
				reselectShelf(myShelf);
				return true;
			}
		}

		@Override
		Set<Integer> extraMenuItemIds() {
			// TODO: implement
			return Collections.emptySet();
		}
	}

	private final class CustomCategoryProvider extends FilterProvider {
		final String myLabel;

		CustomCategoryProvider(Shelf.CustomShelf item) {
			super(item.filter(), TITLE_COMPARATOR);
			myLabel = item.Label;
		}

		@Override
		SparseArray<BookActionMenu.Action> extraActions() {
			return SparseArrayUtil.<BookActionMenu.Action>singleton(
				R.id.bks_book_action_remove_from_current_shelf,
				new BookActionMenu.Action() {
					@Override
					void run(LibraryActivity activity, Book book) {
						book.removeLabel(myLabel);
						activity.Collection.saveBook(book);
					}

					@Override
					boolean closePopup() {
						return true;
					}

					@Override
					String updatedTitle(String title) {
						return title.replace("%s", BookUtil.customCategoryTitle(myLabel));
					}
				}
			);
		}
	}

	private final class AuthorsProvider extends Provider implements Comparator<Object> {
		@Override
		SectionIndexer sectionIndexer() {
			// TODO: indexer
			return null;
		}

		@Override
		SetupAction setupAction() {
			return new SetupAction() {
				public SetupAction run() {
					synchronized (myItemList) {
						myItemList.clear();
						myItemList.addAll(myActivity.Collection.authors());
						myItemList.remove(Author.NULL);
						Collections.sort(myItemList, AuthorsProvider.this);
					}
					notifyDataSetChanged();
					myActivity.invalidateGrid();
					return null;
				}
			};
		}

		@Override
		boolean onBookEventList(List<Pair<BookEvent,Book>> events) {
			final List<Author> toAdd = new ArrayList<Author>();

			final HashSet<Author> alreadyInUse = new HashSet<Author>(myItemList);
			for (Pair<BookEvent,Book> e : events) {
				if (e.First != BookEvent.Added) {
					continue;
				}

				for (Author a : e.Second.authors()) {
					if (!Author.NULL.equals(a) && !alreadyInUse.contains(a)) {
						alreadyInUse.add(a);
						toAdd.add(a);
					}
				}
			}
			if (toAdd.isEmpty()) {
				return false;
			}

			synchronized (myItemList) {
				boolean updated = false;
				for (Author a : toAdd) {
					final int index =
						Collections.binarySearch(myItemList, a, AuthorsProvider.this);
					if (index < 0) {
						myItemList.add(- index - 1, a);
						updated = true;
					}
				}
				return updated;
			}
		}

		public int compare(Object o0, Object o1) {
			return ((Author)o0).SortKey.compareTo(((Author)o1).SortKey);
		}
	}

	private final class SeriesProvider extends Provider implements Comparator<Object> {
		@Override
		SectionIndexer sectionIndexer() {
			// TODO: indexer
			return null;
		}

		@Override
		SetupAction setupAction() {
			return new SetupAction() {
				public SetupAction run() {
					synchronized (myItemList) {
						myItemList.clear();
						myItemList.addAll(myActivity.Collection.series());
						Collections.sort(myItemList, SeriesProvider.this);
					}
					notifyDataSetChanged();
					myActivity.invalidateGrid();
					return null;
				}
			};
		}

		@Override
		boolean onBookEventList(List<Pair<BookEvent,Book>> events) {
			final List<String> toAdd = new ArrayList<String>();

			final HashSet<String> alreadyInUse = new HashSet<String>(myItemList);
			for (Pair<BookEvent,Book> e : events) {
				if (e.First != BookEvent.Added) {
					continue;
				}

				final SeriesInfo info = e.Second.getSeriesInfo();
				if (info == null) {
					continue;
				}
				final String series = info.Series.getTitle();
				if (!alreadyInUse.contains(series)) {
					alreadyInUse.add(series);
					toAdd.add(series);
				}
			}
			if (toAdd.isEmpty()) {
				return false;
			}
			synchronized (myItemList) {
				boolean updated = false;
				for (String series : toAdd) {
					final int index =
						Collections.binarySearch(myItemList, series, SeriesProvider.this);
					if (index < 0) {
						myItemList.add(- index - 1, series);
						updated = true;
					}
				}
				return updated;
			}
		}

		public int compare(Object o0, Object o1) {
			return NATURAL_ORDER_COMPARATOR.compare((String)o0, (String)o1);
		}
	}

	private void executeSetupAction(final Provider provider, final SetupAction action) {
		myActivity.runOnUiThread(new Runnable() {
			public void run() {
				synchronized (BooksAdapter.this) {
					if (provider != myProvider) {
						return;
					}

					final SetupAction next = action.run();
					if (next != null) {
						BookUtil.ThreadPool.execute(new Runnable() {
							public void run() {
								executeSetupAction(provider, next);
							}
						});
					} else {
						PositionManager.selectItem();
					}
				}
			}
		});
	}

	private synchronized void setProvider(final Provider provider) {
		myProvider = provider;
		myActivity.applyViewType(provider.viewType());
		executeSetupAction(provider, provider.setupAction());
		myActivity.supportInvalidateOptionsMenu();
		BookUtil.CoversCache.removeNulls();
		myActivity.mainView().setFastScrollEnabled(provider.sectionIndexer() != null);
	}

	private void setProvider(Filter filter, Comparator<Book> comparator) {
		setProvider(new FilterProvider(filter, comparator));
	}

	private final List<Pair<Pair<BookEvent,Book>,Provider>> myEventQueue =
		Collections.synchronizedList(new LinkedList<Pair<Pair<BookEvent,Book>,Provider>>());
	private final Runnable myEventQueueHandler = new Runnable() {
		public void run() {
			final List<Pair<Pair<BookEvent,Book>,Provider>> queue;
			synchronized (myEventQueue) {
				queue = new ArrayList<Pair<Pair<BookEvent,Book>,Provider>>(myEventQueue);
				myEventQueue.clear();
			}

			final Set<String> names = new HashSet<String>();
			for (Pair<Pair<BookEvent,Book>,Provider> pair : queue) {
				final Pair<BookEvent,Book> event = pair.First;
				switch (event.First) {
					case Added:
					case Updated:
						for (Label l : event.Second.labels()) {
							names.add(l.Name);
						}
						break;
					default:
						break;
				}
			}
			myActivity.updateCustomCategoryList(names);

			synchronized (BooksAdapter.this) {
				final List<Pair<BookEvent,Book>> events =
					new ArrayList<Pair<BookEvent,Book>>();
				for (Pair<Pair<BookEvent,Book>,Provider> pair : queue) {
					if (pair.Second == myProvider) {
						events.add(pair.First);
					}
				}
				if (!events.isEmpty() && myProvider.onBookEventList(events)) {
					notifyDataSetChanged();
					myActivity.invalidateGrid();
				}
			}
		}
	};

	@Override
	public synchronized void onBookEvent(BookEvent event, Book book) {
		synchronized (myEventQueue) {
			myEventQueue.add(new Pair(new Pair(event, book), myProvider));
			if (myEventQueue.size() == 1) {
				myActivity.mainView().postDelayed(myEventQueueHandler, 100);
			}
		}
		invalidateStateCache();
	}

	void invalidateStateCache() {
		synchronized (myCategoryStateCache) {
			myCategoryStateCache.clear();
			myActivity.invalidateDrawer();
		}
	}

	@Override
	public void onBuildEvent(IBookCollection.Status status) {
		myActivity.setProgressVisibility(!status.IsComplete);
	}

	boolean isShelfActive(Shelf item) {
		Boolean value = myCategoryStateCache.get(item);
		if (value == null) {
			value = item.isActive(myActivity.Collection, myActivity.preferences());
			myCategoryStateCache.put(item, value);
		}
		return value;
	}

	void selectSavedCategory() {
		try {
			selectShelfInternal(Shelf.read(myActivity.preferences()), false);
		} catch (Exception e) {
			selectShelfInternal(Shelf.defaultShelf(Shelf.Category.RecentlyOpened), true);
		}
	}

	void selectShelf(Shelf shelf) {
		selectShelfInternal(shelf, true);
	}

	final class PositionManager {
		private Map<String,Integer> myPositionStore = new HashMap<String,Integer>();
		private volatile String myCategory;

		synchronized void setCategory(String category) {
			myCategory = category;
		}

		private synchronized int getPosition() {
			Integer pos = myPositionStore.get(myCategory);
			if (pos == null) {
				pos = myActivity.preferences().getInt("position:" + myCategory, 0);
				myPositionStore.put(myCategory, pos);
			}
			return pos;
		}

		synchronized void selectItem() {
			if (myCategory != null) {
				myActivity.selectItemInternal(getPosition());
			}
		}

		synchronized void storePosition(int position) {
			if (myCategory != null) {
				int old = getPosition();
				if (position != old) {
					myPositionStore.put(myCategory, position);
					final SharedPreferences.Editor edit = myActivity.preferences().edit();
					edit.putInt("position:" + myCategory, position);
					edit.apply();
				}
			}
		}
	};

	final PositionManager PositionManager = new PositionManager();

	private void selectShelfInternal(Shelf shelf, boolean save) {
		PositionManager.setCategory(null);

		switch (shelf.Kind) {
			default:
				throw new RuntimeException("Unknown category: " + shelf.Kind);
			case Author:
				setProvider(new SingleAuthorProvider((Shelf.AuthorShelf)shelf));
				break;
			case Series:
				setProvider(new SingleSeriesProvider((Shelf.SeriesShelf)shelf));
				break;
			case AllTitles:
				setProvider(new Filter.Empty(), TITLE_COMPARATOR);
				break;
			case Favorites:
				setProvider(new Filter.ByLabel(Book.FAVORITE_LABEL), TITLE_COMPARATOR);
				break;
			case RecentlyAdded:
				setProvider(new RecentlyAddedProvider((Shelf.RecentlyAddedShelf)shelf));
				break;
			case RecentlyOpened:
				setProvider(new RecentlyOpenedProvider((Shelf.RecentlyOpenedShelf)shelf));
				break;
			case AllAuthors:
				setProvider(new AuthorsProvider());
				break;
			case AllSeries:
				setProvider(new SeriesProvider());
				break;
			case Found:
				{
					final Filter filter = ((Shelf.SearchShelf)shelf).filter();
					if (filter == null) {
						throw new RuntimeException("empty search parameter");
					}
					setProvider(filter, null);
				}
				break;
			case FileSystem:
				setProvider(new FileSystemProvider((Shelf.FileSystemShelf)shelf));
				break;
			case Custom:
				setProvider(new CustomCategoryProvider((Shelf.CustomShelf)shelf));
				break;
		}

		myActivity.setTitle(shelf.windowTitle(myActivity));

		if (save) {
			shelf.save(myActivity.preferences());
		}
		PositionManager.setCategory(shelf.keyString());
		PositionManager.selectItem();
	}

	private void reselectShelf(Shelf shelf) {
		PositionManager.setCategory(null);

		executeSetupAction(myProvider, myProvider.setupAction());
		myActivity.supportInvalidateOptionsMenu();
		BookUtil.CoversCache.removeNulls();

		myActivity.setTitle(shelf.windowTitle(myActivity));

		PositionManager.setCategory(shelf.keyString());
		PositionManager.selectItem();
	}

	private volatile boolean myWatchScroll = false;

	@Override
	public void onScroll(AbsListView view, int firstVisible, int visible, int total) {
		if (myWatchScroll) {
			PositionManager.storePosition(firstVisible);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		myWatchScroll = scrollState != SCROLL_STATE_IDLE;
	}

	Set<Integer> extraMenuItemIds() {
		return myProvider != null ? myProvider.extraMenuItemIds() : Collections.<Integer>emptySet();
	}

	boolean onBackPressed() {
		return myProvider != null ? myProvider.onBackPressed() : false;
	}

	void reset(final Runnable postAction) {
		myActivity.runOnUiThread(new Runnable() {
			public void run() {
				synchronized (BooksAdapter.this) {
					if (myProvider != null) {
						BookUtil.CoversCache.removeNulls();
						myProvider.reset();
						notifyDataSetChanged();
						myActivity.invalidateGrid();
						if (postAction != null) {
							postAction.run();
						}
					}
				}
			}
		});
	}

	public int getPositionForSection(int sectionIndex) {
		final SectionIndexer indexer = myProvider.sectionIndexer();
		return indexer != null ? indexer.getPositionForSection(sectionIndex) : 0;
	}

	public int getSectionForPosition(int positionIndex) {
		final SectionIndexer indexer = myProvider.sectionIndexer();
		return indexer != null ? indexer.getSectionForPosition(positionIndex) : 0;
	}

	public Object[] getSections() {
		final SectionIndexer indexer = myProvider.sectionIndexer();
		return indexer != null ? indexer.getSections() : new Object[0];
	}
}
