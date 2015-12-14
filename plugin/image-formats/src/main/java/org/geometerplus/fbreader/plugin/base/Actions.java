package org.geometerplus.fbreader.plugin.base;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import android.widget.Toast;

import org.fbreader.reader.TOCTree;
import org.fbreader.reader.TOCTreeUtil;
import org.fbreader.reader.android.GotoPageDialogUtil;
import org.fbreader.util.Boolean3;

import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.plugin.base.optiondialogs.*;
import org.geometerplus.fbreader.plugin.base.reader.PluginView;
import org.geometerplus.fbreader.plugin.base.tree.TOCActivity;
import org.geometerplus.fbreader.util.FixedTextSnippet;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.dict.DictionaryUtil;

public class Actions {
	static class StartSearchAction extends ViewHolder.Action<ViewHolder> {
		protected StartSearchAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().openSearchView();
		}
	}

	static class StopSearchAction extends ViewHolder.Action<ViewHolder> {
		protected StopSearchAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().stopSearch();
		}
	}

	static class FindNextAction extends ViewHolder.Action<ViewHolder> {
		protected FindNextAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		public boolean isEnabled() {
			return Reader.getActivity().getPluginView().canFindNext();
		}

		@Override
		protected void run(Object... params) {
			final String message = Util.getResourceString("dialog", "waitMessage", "search");
			final ProgressDialog progress = ProgressDialog.show(
				Reader.getActivity(), null, message, true, false
			);
			PluginView.AuxService.execute(new Runnable() {
				public void run() {
					Reader.getActivity().getPluginView().findNext();
					Reader.getActivity().runOnUiThread(new Runnable() {
						public void run() {
							progress.dismiss();
							Reader.getActivity().onPageChanged();
						}
					});
				}
			});
		}
	}

	static class FindPrevAction extends ViewHolder.Action<ViewHolder> {
		protected FindPrevAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		public boolean isEnabled() {
			return Reader.getActivity().getPluginView().canFindPrev();
		}

		@Override
		protected void run(Object... params) {
			final String message = Util.getResourceString("dialog", "waitMessage", "search");
			final ProgressDialog progress = ProgressDialog.show(
				Reader.getActivity(), null, message, true, false
			);
			PluginView.AuxService.execute(new Runnable() {
				public void run() {
					Reader.getActivity().getPluginView().findPrev();
					Reader.getActivity().runOnUiThread(new Runnable() {
						public void run() {
							progress.dismiss();
							Reader.getActivity().onPageChanged();
						}
					});
				}
			});
		}
	}

	static class NavigateAction extends ViewHolder.Action<ViewHolder> {
		protected NavigateAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().toggleBars();
		}
	}

	static class ClearSelectionAction extends ViewHolder.Action<ViewHolder> {
		protected ClearSelectionAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().clearSelection();
		}
	}

	static class CopySelectionAction extends ViewHolder.Action<ViewHolder> {
		protected CopySelectionAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		private void copySelectionOld() {
			final android.text.ClipboardManager clipboard =
				(android.text.ClipboardManager)Reader.getActivity().getSystemService(Application.CLIPBOARD_SERVICE);
			clipboard.setText(Reader.getSelectedText());
			Toast.makeText(
				Reader.getActivity(),
				Util.getResourceString("selection", "textInBuffer")
					.replace("%s", clipboard.getText()),
				Toast.LENGTH_SHORT
			).show();
			Reader.getActivity().getPluginView().clearSelection();
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		private void copySelectionNew() {
			final android.content.ClipboardManager clipboard =
				(android.content.ClipboardManager)Reader.getActivity().getSystemService(Application.CLIPBOARD_SERVICE);
			clipboard.setPrimaryClip(ClipData.newPlainText("FBReader", Reader.getSelectedText()));
			Toast.makeText(
				Reader.getActivity(),
				Util.getResourceString("selection", "textInBuffer")
					.replace("%s", clipboard.getText()),
				Toast.LENGTH_SHORT
			).show();
			Reader.getActivity().getPluginView().clearSelection();
		}

		@Override
		protected void run(Object... params) {//TODO
			if (android.os.Build.VERSION.SDK_INT >= 11) {
				copySelectionNew();
			} else {
				copySelectionOld();
			}
		}
	}

	static class ShareSelectionAction extends ViewHolder.Action<ViewHolder> {
		protected ShareSelectionAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final Intent i = new Intent(android.content.Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(
				android.content.Intent.EXTRA_SUBJECT,
				Util.getResourceString("selection", "quoteFrom")
					.replace("%s", Reader.getCurrentBook().getTitle())
			);
			i.putExtra(android.content.Intent.EXTRA_TEXT, Reader.getSelectedText());
			Reader.getActivity().startActivity(Intent.createChooser(i, null));
			Reader.getActivity().getPluginView().clearSelection();
		}
	}

	static class TranslateSelectionAction extends ViewHolder.Action<ViewHolder> {
		protected TranslateSelectionAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final String text = Reader.getSelectedText();
			DictionaryUtil.openTextInDictionary(
				Reader.getActivity(),
				text,
				!containsWhitespace(text), // is single word
				100, // top
				200, // bottom
				null // outliner
			);
			Reader.getActivity().getPluginView().clearSelection();
		}
	}

	private static boolean containsWhitespace(String text) {
		for (int i = 0; i < text.length(); ++i) {
			final char ch = text.charAt(i);
			if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) {
				return true;
			}
		}
		return false;
	}

	static class BookmarkSelectionAction extends ViewHolder.Action<ViewHolder> {
		protected BookmarkSelectionAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			String text = Reader.getSelectedText();
			int start = Reader.getSelectionStart();
			int end = Reader.getSelectionEnd();
			int length = Reader.getSelectedPageLength();
			int page1 = Reader.getActivity().getPluginView().getCurPageNo();
			int page2 = page1;
			if (end > length) {
				page2 = page1 + 1;
				end -= length;
			}
			if (start > length) {
				page1 = page1 + 1;
				start -= length;
			}
			final Bookmark bookmark = new Bookmark(
				Reader.Collection,
				Reader.getCurrentBook(), "",
				new FixedTextSnippet(
					new ZLTextFixedPosition(page1, start, 0),
					new ZLTextFixedPosition(page2, end, 0),
					text
				),
				true
			);
			Reader.saveBookmark(bookmark);
		}
	}

	public static boolean canBeStarted(Context context, Intent intent) {
		final PackageManager manager = context.getApplicationContext().getPackageManager();
		final ResolveInfo info =
			manager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (info == null) {
			return false;
		}
		final ActivityInfo activityInfo = info.activityInfo;
		if (activityInfo == null) {
			return false;
		}
		return
			PackageManager.SIGNATURE_MATCH ==
			manager.checkSignatures(context.getPackageName(), activityInfo.packageName);
	}

	static class OpenLibraryAction extends ViewHolder.Action<ViewHolder> {
		protected OpenLibraryAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final Intent externalIntent =
				new Intent(FBReaderIntents.Action.EXTERNAL_LIBRARY)
					.addCategory(Intent.CATEGORY_DEFAULT);
			FBReaderIntents.putBookExtra(externalIntent, Reader.getCurrentBook());
			final Intent internalIntent =
				FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.LIBRARY);
			FBReaderIntents.putBookExtra(internalIntent, Reader.getCurrentBook());

			if (canBeStarted(Reader.getActivity(), externalIntent)) {
				try {
					Reader.getActivity().startActivity(externalIntent);
				} catch (ActivityNotFoundException e) {
					Reader.getActivity().startActivity(internalIntent);
				}
			} else {
				Reader.getActivity().startActivity(internalIntent);
			}
			Reader.getActivity().overridePendingTransition(0, 0);
		}
	}

	static class OpenSettingsAction extends ViewHolder.Action<ViewHolder> {
		protected OpenSettingsAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final Intent intent =
				FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.PREFERENCES);
			Reader.getActivity().startActivity(intent);
			Reader.getActivity().overridePendingTransition(0, 0);
			Reader.getActivity().finish();
			Reader.getActivity().overridePendingTransition(0, 0);
		}
	}

	static class OpenBookmarksAction extends ViewHolder.Action<ViewHolder> {
		protected OpenBookmarksAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final PluginView view = Reader.getActivity().getPluginView();
			final String text = view.getPageStartText();
			final int page = view.getCurPageNo();
			final Book book = Reader.getCurrentBook();
			final Bookmark bookmark = new Bookmark(
				Reader.Collection,
				book, "",
				new FixedTextSnippet(
					new ZLTextFixedPosition(page, 0, 0),
					new ZLTextFixedPosition(page, text.length(), 0),
					text
				),
				true
			);

			final Intent externalIntent =
				new Intent(FBReaderIntents.Action.EXTERNAL_BOOKMARKS)
					.addCategory(Intent.CATEGORY_DEFAULT);
			FBReaderIntents.putBookExtra(externalIntent, book);
			FBReaderIntents.putBookmarkExtra(externalIntent, bookmark);

			final Intent internalIntent =
				FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.BOOKMARKS);
			FBReaderIntents.putBookExtra(internalIntent, book);
			FBReaderIntents.putBookmarkExtra(internalIntent, bookmark);

			if (canBeStarted(Reader.getActivity(), externalIntent)) {
				try {
					Reader.getActivity().startActivity(externalIntent);
				} catch (ActivityNotFoundException e) {
					Reader.getActivity().startActivity(internalIntent);
				}
			} else {
				Reader.getActivity().startActivity(internalIntent);
			}
			Reader.getActivity().overridePendingTransition(0, 0);
		}
	}

	static class OpenBookInfoAction extends ViewHolder.Action<ViewHolder> {
		protected OpenBookInfoAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final Intent i = FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.BOOK_INFO);
			FBReaderIntents.putBookExtra(i, Reader.getCurrentBook());
			i.putExtra("fromReadingMode", true);
			Reader.startActivity(i);
		}
	}

	static class ShowTOCAction extends ViewHolder.Action<ViewHolder> {
		protected ShowTOCAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		public boolean isEnabled() {
			final TOCTree tocTree = Reader.getActivity().getPluginView().getTOCTree();
			return tocTree != null && tocTree.hasChildren();
		}

		@Override
		protected void run(Object... params) {
			Intent i = new Intent(Reader.getActivity(), TOCActivity.class);
			i.putExtra(
				TOCActivity.TOCTREE_KEY, TOCTreeUtil.toJSONObject(Reader.getView().getTOCTree())
			);
			i.putExtra(TOCActivity.PAGENO_KEY, Reader.getView().getCurPageNo());
			Reader.getActivity().startActivityForResult(i, FBReaderPluginActivity.REQUEST_TOC);
		}
	}

	static class SwitchProfileAction extends ViewHolder.Action<ViewHolder> {
		private final SettingsHolder mySettings;
		private final String myProfileName;

		protected SwitchProfileAction(ViewHolder viewHolder, SettingsHolder settings, String profileName) {
			super(viewHolder);
			mySettings = settings;
			myProfileName = profileName;
		}

		@Override
		protected void run(Object... params) {
			mySettings.ColorProfileName.setValue(myProfileName);
			Reader.getActivity().getPluginView().resetNightMode();
			Reader.getActivity().settingsCalled();//FIXME: obtain only colors
			Reader.getActivity().hideBars();
			Reader.getActivity().refresh();
		}

		@Override
		public boolean isEnabled() {
			return !myProfileName.equals(mySettings.ColorProfileName.getValue());
		}
	}

	static class ZoomInAction extends ViewHolder.Action<ViewHolder> {
		protected ZoomInAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().zoomIn();
		}

		@Override
		public boolean isEnabled() {
			return Reader.getActivity().getPluginView().getZoomMode().Mode == PluginView.ZoomMode.FREE_ZOOM;
		}
	}

	static class ZoomOutAction extends ViewHolder.Action<ViewHolder> {
		protected ZoomOutAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().zoomOut();
		}

		@Override
		public boolean isEnabled() {
			return Reader.getActivity().getPluginView().getZoomMode().Mode == PluginView.ZoomMode.FREE_ZOOM;
		}
	}

	static class NextPageAction extends ViewHolder.Action<ViewHolder> {
		protected NextPageAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().gotoNextPage();
		}
	}

	static class PrevPageAction extends ViewHolder.Action<ViewHolder> {
		protected PrevPageAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().gotoPrevPage();
		}
	}

	static class ShowMenuAction extends ViewHolder.Action<ViewHolder> {
		protected ShowMenuAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().openOptionsMenu();
		}
	}

	static class CancelMenuAction extends ViewHolder.Action<ViewHolder> {
		protected CancelMenuAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			if (Reader.getActivity().hideSearchItem()) {
				return;
			}

			Reader.getActivity().startActivityForResult(
				FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.CANCEL_MENU),
				FBReaderPluginActivity.REQUEST_CANCEL_MENU
			);
		}
	}

	static class ExitAction extends ViewHolder.Action<ViewHolder> {
		protected ExitAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final Intent i = FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.CLOSE);
			i.putExtra(FBReaderIntents.Key.TYPE, "close");
			Reader.startActivity(i);
		}
	}

	static class SetScreenOrientationAction extends ViewHolder.Action<ViewHolder> {
		private final String myValue;

		protected SetScreenOrientationAction(ViewHolder viewHolder, String value) {
			super(viewHolder);
			myValue = value;
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getZLibrary().getOrientationOption().setValue(myValue);
			Reader.onSettingsChange();
		}

		@Override
		public Boolean3 isChecked() {
			if (myValue.equals(Reader.getActivity().getZLibrary().getOrientationOption().getValue())) {
				return Boolean3.TRUE;
			}
			return Boolean3.FALSE;
		}
	}

	static class CropAction extends ViewHolder.Action<ViewHolder> {
		protected CropAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final FBReaderPluginActivity activity = Reader.getActivity();
			activity.hideBars();
			new CropDialog(activity, activity.getPluginView().getDocument().getCropInfo()).show();
		}
	}

	static class ZoomModeAction extends ViewHolder.Action<ViewHolder> {
		protected ZoomModeAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final FBReaderPluginActivity activity = Reader.getActivity();
			activity.hideBars();
			new ZoomModeDialog(activity, activity.getPluginView().getZoomMode()).show();
		}
	}

	static class IntersectionAction extends ViewHolder.Action<ViewHolder> {
		protected IntersectionAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final FBReaderPluginActivity activity = Reader.getActivity();
			activity.hideBars();
			new IntersectionDialog(activity, activity.getPluginView().getIntersections()).show();
		}
	}

	static class PageWayAction extends ViewHolder.Action<ViewHolder> {
		protected PageWayAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final FBReaderPluginActivity activity = Reader.getActivity();
			activity.hideBars();
			new PageWayDialog(activity, activity.getPluginView().isHorizontalFirst()).show();
		}
	}

	static class UseBackgroundAction extends ViewHolder.Action<ViewHolder> {
		protected UseBackgroundAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			Reader.getActivity().getPluginView().useWallPaper(isChecked() == Boolean3.FALSE);
		}

		@Override
		public Boolean3 isChecked() {
			if (Reader.getActivity().getPluginView().useWallPaper()) {
				return Boolean3.TRUE;
			}
			return Boolean3.FALSE;
		}
	}

	static class GotoPageNumberAction extends ViewHolder.Action<ViewHolder> {
		protected GotoPageNumberAction(ViewHolder viewHolder) {
			super(viewHolder);
		}

		@Override
		protected void run(Object... params) {
			final PluginView view = Reader.getActivity().getPluginView();
			GotoPageDialogUtil.showDialog(
				Reader.getActivity(),
				new GotoPageDialogUtil.PageSelector() {
					@Override
					public void gotoPage(int page) {
						view.gotoPage(page - 1, false);
						Reader.getActivity().hideBars();
					}
				},
				view.getCurPageNo() + 1,
				view.getPagesNum()
			);

			Reader.getActivity().hideBars();
		}
	}
}
