package org.geometerplus.zlibrary.core.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Some CSS style definition.
 *
 * @Version: 1.0
 */
public class CSSUtil {

    /**
     * Color name: <a href ="CSS 颜色名"> https://www.w3school.com.cn/cssref/css_colornames.asp </>
     */
    public static final HashMap<String, String> COLOR_MAP = new HashMap<>();
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
            if (COLOR_MAP.containsKey(lowerColor)) {
                return COLOR_MAP.get(lowerColor);
            } else if (lowerColor.startsWith(COLOR_FLAG)) {
                return color;
            }
            if (KEYWORD.KEYWORDS.contains(lowerColor)) {
                return color;
            }
        }
        return "";
    }

    static {
        COLOR_MAP.put("AliceBlue".toLowerCase(), "#F0F8FF");
        COLOR_MAP.put("AntiqueWhite".toLowerCase(), "#FAEBD7");
        COLOR_MAP.put("Aqua".toLowerCase(), "#00FFFF");
        COLOR_MAP.put("Aquamarine".toLowerCase(), "#7FFFD4");
        COLOR_MAP.put("Azure".toLowerCase(), "#F0FFFF");
        COLOR_MAP.put("Beige".toLowerCase(), "#F5F5DC");
        COLOR_MAP.put("Bisque".toLowerCase(), "#FFE4C4");
        COLOR_MAP.put("Black".toLowerCase(), "#000000");
        COLOR_MAP.put("BlanchedAlmond".toLowerCase(), "#FFEBCD");
        COLOR_MAP.put("Blue".toLowerCase(), "#0000FF");
        COLOR_MAP.put("BlueViolet".toLowerCase(), "#8A2BE2");
        COLOR_MAP.put("Brown".toLowerCase(), "#A52A2A");
        COLOR_MAP.put("BurlyWood".toLowerCase(), "#DEB887");
        COLOR_MAP.put("CadetBlue".toLowerCase(), "#5F9EA0");
        COLOR_MAP.put("Chartreuse".toLowerCase(), "#7FFF00");
        COLOR_MAP.put("Chocolate".toLowerCase(), "#D2691E");
        COLOR_MAP.put("Coral".toLowerCase(), "#FF7F50");
        COLOR_MAP.put("CornflowerBlue".toLowerCase(), "#6495ED");
        COLOR_MAP.put("Cornsilk".toLowerCase(), "#FFF8DC");
        COLOR_MAP.put("Crimson".toLowerCase(), "#DC143C");
        COLOR_MAP.put("Cyan".toLowerCase(), "#00FFFF");
        COLOR_MAP.put("DarkBlue".toLowerCase(), "#00008B");
        COLOR_MAP.put("DarkCyan".toLowerCase(), "#008B8B");
        COLOR_MAP.put("DarkGoldenRod".toLowerCase(), "#B8860B");
        COLOR_MAP.put("DarkGray".toLowerCase(), "#A9A9A9");
        COLOR_MAP.put("DarkGreen".toLowerCase(), "#006400");
        COLOR_MAP.put("DarkKhaki".toLowerCase(), "#BDB76B");
        COLOR_MAP.put("DarkMagenta".toLowerCase(), "#8B008B");
        COLOR_MAP.put("DarkOliveGreen".toLowerCase(), "#556B2F");
        COLOR_MAP.put("Darkorange".toLowerCase(), "#FF8C00");
        COLOR_MAP.put("DarkOrchid".toLowerCase(), "#9932CC");
        COLOR_MAP.put("DarkRed".toLowerCase(), "#8B0000");
        COLOR_MAP.put("DarkSalmon".toLowerCase(), "#E9967A");
        COLOR_MAP.put("DarkSeaGreen".toLowerCase(), "#8FBC8F");
        COLOR_MAP.put("DarkSlateBlue".toLowerCase(), "#483D8B");
        COLOR_MAP.put("DarkSlateGray".toLowerCase(), "#2F4F4F");
        COLOR_MAP.put("DarkTurquoise".toLowerCase(), "#00CED1");
        COLOR_MAP.put("DarkViolet".toLowerCase(), "#9400D3");
        COLOR_MAP.put("DeepPink".toLowerCase(), "#FF1493");
        COLOR_MAP.put("DeepSkyBlue".toLowerCase(), "#00BFFF");
        COLOR_MAP.put("DimGray".toLowerCase(), "#696969");
        COLOR_MAP.put("DodgerBlue".toLowerCase(), "#1E90FF");
        COLOR_MAP.put("Feldspar".toLowerCase(), "#D19275");
        COLOR_MAP.put("FireBrick".toLowerCase(), "#B22222");
        COLOR_MAP.put("FloralWhite".toLowerCase(), "#FFFAF0");
        COLOR_MAP.put("ForestGreen".toLowerCase(), "#228B22");
        COLOR_MAP.put("Fuchsia".toLowerCase(), "#FF00FF");
        COLOR_MAP.put("Gainsboro".toLowerCase(), "#DCDCDC");
        COLOR_MAP.put("GhostWhite".toLowerCase(), "#F8F8FF");
        COLOR_MAP.put("Gold".toLowerCase(), "#FFD700");
        COLOR_MAP.put("GoldenRod".toLowerCase(), "#DAA520");
        COLOR_MAP.put("Gray".toLowerCase(), "#808080");
        COLOR_MAP.put("Green".toLowerCase(), "#008000");
        COLOR_MAP.put("GreenYellow".toLowerCase(), "#ADFF2F");
        COLOR_MAP.put("HoneyDew".toLowerCase(), "#F0FFF0");
        COLOR_MAP.put("HotPink".toLowerCase(), "#FF69B4");
        COLOR_MAP.put("IndianRed".toLowerCase(), "#CD5C5C");
        COLOR_MAP.put("Indigo".toLowerCase(), "#4B0082");
        COLOR_MAP.put("Ivory".toLowerCase(), "#FFFFF0");
        COLOR_MAP.put("Khaki".toLowerCase(), "#F0E68C");
        COLOR_MAP.put("Lavender".toLowerCase(), "#E6E6FA");
        COLOR_MAP.put("LavenderBlush".toLowerCase(), "#FFF0F5");
        COLOR_MAP.put("LawnGreen".toLowerCase(), "#7CFC00");
        COLOR_MAP.put("LemonChiffon".toLowerCase(), "#FFFACD");
        COLOR_MAP.put("LightBlue".toLowerCase(), "#ADD8E6");
        COLOR_MAP.put("LightCoral".toLowerCase(), "#F08080");
        COLOR_MAP.put("LightCyan".toLowerCase(), "#E0FFFF");
        COLOR_MAP.put("LightGoldenRodYellow".toLowerCase(), "#FAFAD2");
        COLOR_MAP.put("LightGrey".toLowerCase(), "#D3D3D3");
        COLOR_MAP.put("LightGreen".toLowerCase(), "#90EE90");
        COLOR_MAP.put("LightPink".toLowerCase(), "#FFB6C1");
        COLOR_MAP.put("LightSalmon".toLowerCase(), "#FFA07A");
        COLOR_MAP.put("LightSeaGreen".toLowerCase(), "#20B2AA");
        COLOR_MAP.put("LightSkyBlue".toLowerCase(), "#87CEFA");
        COLOR_MAP.put("LightSlateBlue".toLowerCase(), "#8470FF");
        COLOR_MAP.put("LightSlateGray".toLowerCase(), "#778899");
        COLOR_MAP.put("LightSteelBlue".toLowerCase(), "#B0C4DE");
        COLOR_MAP.put("LightYellow".toLowerCase(), "#FFFFE0");
        COLOR_MAP.put("Lime".toLowerCase(), "#00FF00");
        COLOR_MAP.put("LimeGreen".toLowerCase(), "#32CD32");
        COLOR_MAP.put("Linen".toLowerCase(), "#FAF0E6");
        COLOR_MAP.put("Magenta".toLowerCase(), "#FF00FF");
        COLOR_MAP.put("Maroon".toLowerCase(), "#800000");
        COLOR_MAP.put("MediumAquaMarine".toLowerCase(), "#66CDAA");
        COLOR_MAP.put("MediumBlue".toLowerCase(), "#0000CD");
        COLOR_MAP.put("MediumOrchid".toLowerCase(), "#BA55D3");
        COLOR_MAP.put("MediumPurple".toLowerCase(), "#9370D8");
        COLOR_MAP.put("MediumSeaGreen".toLowerCase(), "#3CB371");
        COLOR_MAP.put("MediumSlateBlue".toLowerCase(), "#7B68EE");
        COLOR_MAP.put("MediumSpringGreen".toLowerCase(), "#00FA9A");
        COLOR_MAP.put("MediumTurquoise".toLowerCase(), "#48D1CC");
        COLOR_MAP.put("MediumVioletRed".toLowerCase(), "#C71585");
        COLOR_MAP.put("MidnightBlue".toLowerCase(), "#191970");
        COLOR_MAP.put("MintCream".toLowerCase(), "#F5FFFA");
        COLOR_MAP.put("MistyRose".toLowerCase(), "#FFE4E1");
        COLOR_MAP.put("Moccasin".toLowerCase(), "#FFE4B5");
        COLOR_MAP.put("NavajoWhite".toLowerCase(), "#FFDEAD");
        COLOR_MAP.put("Navy".toLowerCase(), "#000080");
        COLOR_MAP.put("OldLace".toLowerCase(), "#FDF5E6");
        COLOR_MAP.put("Olive".toLowerCase(), "#808000");
        COLOR_MAP.put("OliveDrab".toLowerCase(), "#6B8E23");
        COLOR_MAP.put("Orange".toLowerCase(), "#FFA500");
        COLOR_MAP.put("OrangeRed".toLowerCase(), "#FF4500");
        COLOR_MAP.put("Orchid".toLowerCase(), "#DA70D6");
        COLOR_MAP.put("PaleGoldenRod".toLowerCase(), "#EEE8AA");
        COLOR_MAP.put("PaleGreen".toLowerCase(), "#98FB98");
        COLOR_MAP.put("PaleTurquoise".toLowerCase(), "#AFEEEE");
        COLOR_MAP.put("PaleVioletRed".toLowerCase(), "#D87093");
        COLOR_MAP.put("PapayaWhip".toLowerCase(), "#FFEFD5");
        COLOR_MAP.put("PeachPuff".toLowerCase(), "#FFDAB9");
        COLOR_MAP.put("Peru".toLowerCase(), "#CD853F");
        COLOR_MAP.put("Pink".toLowerCase(), "#FFC0CB");
        COLOR_MAP.put("Plum".toLowerCase(), "#DDA0DD");
        COLOR_MAP.put("PowderBlue".toLowerCase(), "#B0E0E6");
        COLOR_MAP.put("Purple".toLowerCase(), "#800080");
        COLOR_MAP.put("Red".toLowerCase(), "#FF0000");
        COLOR_MAP.put("RosyBrown".toLowerCase(), "#BC8F8F");
        COLOR_MAP.put("RoyalBlue".toLowerCase(), "#4169E1");
        COLOR_MAP.put("SaddleBrown".toLowerCase(), "#8B4513");
        COLOR_MAP.put("Salmon".toLowerCase(), "#FA8072");
        COLOR_MAP.put("SandyBrown".toLowerCase(), "#F4A460");
        COLOR_MAP.put("SeaGreen".toLowerCase(), "#2E8B57");
        COLOR_MAP.put("SeaShell".toLowerCase(), "#FFF5EE");
        COLOR_MAP.put("Sienna".toLowerCase(), "#A0522D");
        COLOR_MAP.put("Silver".toLowerCase(), "#C0C0C0");
        COLOR_MAP.put("SkyBlue".toLowerCase(), "#87CEEB");
        COLOR_MAP.put("SlateBlue".toLowerCase(), "#6A5ACD");
        COLOR_MAP.put("SlateGray".toLowerCase(), "#708090");
        COLOR_MAP.put("Snow".toLowerCase(), "#FFFAFA");
        COLOR_MAP.put("SpringGreen.toLowerCase()", "#00FF7F");
        COLOR_MAP.put("SteelBlue".toLowerCase(), "#4682B4");
        COLOR_MAP.put("Tan".toLowerCase(), "#D2B48C");
        COLOR_MAP.put("Teal".toLowerCase(), "#008080");
        COLOR_MAP.put("Thistle".toLowerCase(), "#D8BFD8");
        COLOR_MAP.put("Tomato".toLowerCase(), "#FF6347");
        COLOR_MAP.put("Turquoise".toLowerCase(), "#40E0D0");
        COLOR_MAP.put("Violet".toLowerCase(), "#EE82EE");
        COLOR_MAP.put("VioletRed".toLowerCase(), "#D02090");
        COLOR_MAP.put("Wheat".toLowerCase(), "#F5DEB3");
        COLOR_MAP.put("White".toLowerCase(), "#FFFFFF");
        COLOR_MAP.put("WhiteSmoke".toLowerCase(), "#F5F5F5");
        COLOR_MAP.put("Yellow".toLowerCase(), "#FFFF00");
        COLOR_MAP.put("YellowGreen".toLowerCase(), "#9ACD32");
    }
}
