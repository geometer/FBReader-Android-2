package org.fbreader.plugin.library;

import java.util.List;

import android.content.*;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.SparseArray;
import android.view.*;

import org.fbreader.common.android.FBReaderUtil;
import org.geometerplus.fbreader.book.Book;

class BookActionMenu extends PopupMenu {
	static abstract class Action {
		abstract void run(LibraryActivity activity, Book book);

		boolean closePopup() {
			return false;
		}

		String updatedTitle(String title) {
			return title;
		}
	}

	BookActionMenu(final LibraryActivity activity, final BookPopupWindow popup, final Book book, View anchorView, int id, final SparseArray<Action> extraActions) {
		super(activity, anchorView);
		inflate(id);

		final Menu menu = getMenu();
		final SparseArray<Action> actions = new SparseArray<Action>();

		if (book.hasLabel(Book.FAVORITE_LABEL)) {
			actions.put(R.id.bks_book_action_remove_from_favorites, new Action() {
				@Override
				void run(LibraryActivity activity, Book book) {
					book.removeLabel(Book.FAVORITE_LABEL);
					activity.Collection.saveBook(book);
				}
			});
		} else {
			actions.put(R.id.bks_book_action_add_to_favorites, new Action() {
				@Override
				void run(LibraryActivity activity, Book book) {
					book.addNewLabel(Book.FAVORITE_LABEL);
					activity.Collection.saveBook(book);
				}
			});
		}

		final String path = book.getPath();
		if (path.startsWith("/")) {
			actions.put(R.id.bks_book_action_share, new Action() {
				@Override
				void run(LibraryActivity activity, Book book) {
					FBReaderUtil.shareBook(activity, book);
				}
			});
		}
		if (activity.Collection.canRemoveBook(book, true)) {
			actions.put(R.id.bks_book_action_delete, new Action() {
				@Override
				void run(final LibraryActivity activity, final Book book) {
					new AlertDialog.Builder(activity)
						.setTitle(book.getTitle())
						.setMessage(R.string.delete_book_confirmation)
						.setPositiveButton(
							R.string.button_yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									activity.Collection.removeBook(book, true);
									if (popup != null) {
										popup.dismiss();
									}
								}
							}
						)
						.setNegativeButton(R.string.button_no, null)
						.create().show();
				}
			});
		}

		for (int i = extraActions.size() - 1; i >= 0; --i) {
			actions.append(extraActions.keyAt(i), extraActions.valueAt(i));
		}

		final List<String> customCategories = activity.customCategoryList();
		final int newShelfItemId = customCategories.isEmpty()
			? R.id.bks_book_action_add_to_custom_shelf_quick
			: R.id.bks_book_action_create_new_shelf;
		actions.put(newShelfItemId, new Action() {
			@Override
			void run(LibraryActivity activity, Book book) {
				activity.startShelfCreator(book);
			}
		});

		for (int i = actions.size() - 1; i >= 0; --i) {
			final MenuItem item = menu.findItem(actions.keyAt(i));
			if (item != null) {
				final Action a = actions.valueAt(i);
				item.setVisible(true);
				item.setTitle(a.updatedTitle(item.getTitle().toString()));
			}
		}

		if (!customCategories.isEmpty()) {
			final MenuItem customShelvesItem = menu.findItem(R.id.bks_book_action_add_to_custom_shelf);
			customShelvesItem.setVisible(true);
			final Menu customShelvesMenu = customShelvesItem.getSubMenu();
			int index = 0;
			for (final String label : customCategories) {
				final MenuItem shelfItem =
					customShelvesMenu.add(0, index, index + 1, BookUtil.customCategoryTitle(label));
				if (book.hasLabel(label)) {
					shelfItem.setEnabled(false);
				}
				actions.put(index, new Action() {
					@Override
					void run(final LibraryActivity activity, final Book book) {
						activity.addBookToShelf(book, label);
					}
				});
				++index;
			}
		}

		setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				final Action a = actions.get(item.getItemId());
				if (a != null) {
					a.run(activity, book);
					if (popup != null && a.closePopup()) {
						popup.dismiss();
					}
				}
				return true;
			}
		});
	}
}
