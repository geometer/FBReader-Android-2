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

package org.fbreader.reader;

import java.util.*;

import org.json.simple.JSONValue;

public abstract class TOCTreeUtil {
	public static TOCTree findTreeByReference(TOCTree tree, int reference) {
		if (tree == null || !tree.hasChildren()) {
			return tree;
		} else {
			TOCTree found = null;
			for (TOCTree candidate : tree) {
				if (candidate.Reference == null || candidate.Reference == -1) {
					continue;
				}
				if (candidate.Reference > reference) {
					break;
				}
				found = candidate;
			}
			return found;
		}
	}

	public static HashMap<String,Object> toJSONObject(TOCTree tree) {
		final HashMap<String,Object> map = new HashMap<String,Object>();
		if (tree.Text != null) {
			map.put("t", tree.Text);
		}
		if (tree.Reference != null) {
			map.put("r", tree.Reference);
		}
		if (tree.hasChildren()) {
			final List<TOCTree> children = tree.subtrees();
			final List<Object> lst = new ArrayList<Object>(children.size());
			for (TOCTree child : children) {
				lst.add(toJSONObject(child));
			}
			map.put("c", lst);
		}
		return map;
	}

	public static TOCTree fromJSONObject(Map<String,Object> map) {
		return fromJSONObject(map, null);
	}

	private static Integer asInteger(Object object) {
		if (object instanceof Integer) {
			return (Integer)object;
		} else if (object instanceof Long) {
			return (int)(long)(Long)object;
		} else {
			return null;
		}
	}

	private static TOCTree fromJSONObject(Map<String,Object> map, TOCTree parent) {
		if (map == null) {
			return null;
		}

		final TOCTree tree = new TOCTree(parent, (String)map.get("t"), asInteger(map.get("r")));
		final List<Object> lst = (List<Object>)map.get("c");
		if (lst != null) {
			for (Object o : lst) {
				fromJSONObject((Map<String,Object>)o, tree);
			}
		}
		return tree;
	}
}
