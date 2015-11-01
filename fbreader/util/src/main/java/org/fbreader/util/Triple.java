/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.fbreader.util;

public class Triple<T1,T2,T3> {
	public final T1 First;
	public final T2 Second;
	public final T3 Third;

	public Triple(T1 first, T2 second, T3 third) {
		First = first;
		Second = second;
		Third = third;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Triple)) {
			return false;
		}
		final Triple triple = (Triple)other;
		return
			ComparisonUtil.equal(First, triple.First) &&
			ComparisonUtil.equal(Second, triple.Second) &&
			ComparisonUtil.equal(Third, triple.Third);
	}

	@Override
	public int hashCode() {
		return ComparisonUtil.hashCode(First) +
			23 * (ComparisonUtil.hashCode(Second) + 23 * ComparisonUtil.hashCode(Third));
	}
}
