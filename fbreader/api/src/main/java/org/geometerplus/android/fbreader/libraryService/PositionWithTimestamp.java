/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.libraryService;

import android.os.Parcel;
import android.os.Parcelable;

import org.geometerplus.zlibrary.text.view.ZLTextPositionWithTimestamp;

public final class PositionWithTimestamp implements Parcelable {
	public final int ParagraphIndex;
	public final int ElementIndex;
	public final int CharIndex;
	public final long Timestamp;

	public PositionWithTimestamp(ZLTextPositionWithTimestamp pos) {
		this(
			pos.Position.ParagraphIndex,
			pos.Position.ElementIndex,
			pos.Position.CharIndex,
			pos.Timestamp
		);
	}

	private PositionWithTimestamp(int paragraphIndex, int elementIndex, int charIndex, long stamp) {
		ParagraphIndex = paragraphIndex;
		ElementIndex = elementIndex;
		CharIndex = charIndex;
		Timestamp = stamp;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(ParagraphIndex);
		parcel.writeInt(ElementIndex);
		parcel.writeInt(CharIndex);
		parcel.writeLong(Timestamp);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<PositionWithTimestamp> CREATOR =
		new Parcelable.Creator<PositionWithTimestamp>() {
			public PositionWithTimestamp createFromParcel(Parcel parcel) {
				return new PositionWithTimestamp(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readLong());
			}

			public PositionWithTimestamp[] newArray(int size) {
				return new PositionWithTimestamp[size];
			}
		};
}
