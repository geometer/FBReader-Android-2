package org.fbreader.plugin.library.prefs;

import java.util.*;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import org.geometerplus.fbreader.book.IBookCollection;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

import org.fbreader.plugin.library.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class FileFormatsPreference extends MultiChoicePreference {
	private final BookCollectionShadow myCollection;

	public FileFormatsPreference(Context context, BookCollectionShadow collection) {
		super(context);
		myCollection = collection;

		setTitle(R.string.settings_supported_file_formats);
		setDialogTitle(R.string.settings_supported_file_formats);

		final List<IBookCollection.FormatDescriptor> formats = collection.formats();
		if (formats.size() == 0) {
			setSummary(allSummaryId());
			setEnabled(false);
			return;
		}

		final CharSequence[] entries = new CharSequence[formats.size()];
		final CharSequence[] entryValues = new CharSequence[formats.size()];
		final Set<String> values = new HashSet<String>();
		int index = 0;
		for (IBookCollection.FormatDescriptor d : formats) {
			entryValues[index] = d.Id;
			entries[index] = d.Name;
			if (d.IsActive) {
				values.add(d.Id);
			}
			++index;
		}
		setEntries(entries);
		setEntryValues(entryValues);
		setValues(values);
		update();
	}

	@Override
	protected int noneSummaryId() {
		return R.string.settings_supported_file_formats_summary_none;
	}

	@Override
	protected int allSummaryId() {
		return R.string.settings_supported_file_formats_summary_all;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		myCollection.bindToService(getContext(), new Runnable() {
			@Override
			public void run() {
				myCollection.setActiveFormats(new ArrayList<String>(getValues()));
			}
		});
	}
}
