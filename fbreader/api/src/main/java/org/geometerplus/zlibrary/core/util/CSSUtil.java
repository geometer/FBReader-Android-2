package org.geometerplus.zlibrary.core.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Some CSS style definition.
 *
 * @Version: 1.0
 */
public class CSSUtil {

    /**
     * Color name: <a href="https://www.w3school.com.cn/cssref/css_colornames.asp">...</a>
     */
    private static final String COLOR_FLAG = "#";

    public interface KEYWORD {
        String INITIAL = "initial";
        String INHERIT = "inherit";
        String UNSET = "unset";
        String REVERT = "revert";
        List<String> KEYWORDS = new ArrayList<>(Arrays.asList(INITIAL, INHERIT, UNSET, REVERT));
    }

    public static String transferColor(String color) {
        if (!TextUtils.isEmpty(color)) {
            String lowerColor = color.toLowerCase();
            if (lowerColor.startsWith(COLOR_FLAG)) {
                return color;
            }
            if (KEYWORD.KEYWORDS.contains(lowerColor)) {
                return color;
            }
        }
        return "";
    }
}
