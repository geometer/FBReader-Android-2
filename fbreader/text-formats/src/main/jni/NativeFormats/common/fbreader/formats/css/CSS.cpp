//
// Created by JiangXuehao on 2024/11/14.
//
#include "CSS.h"

// 初始化静态成员变量
const std::string CSS::Properties::PAGE_BREAK_BEFORE = "page-break-before";
const std::string CSS::Properties::PAGE_BREAK_AFTER = "page-break-after";
const std::string CSS::Properties::TEXT_ALIGN = "text-align";
const std::string CSS::Properties::TEXT_DECORATION = "text-decoration";
const std::string CSS::Properties::TEXT_INDENT = "text-indent";
const std::string CSS::Properties::FONT_WEIGHT = "font-weight";
const std::string CSS::Properties::FONT_STYLE = "font-style";
const std::string CSS::Properties::FONT_VARIANT = "font-variant";
const std::string CSS::Properties::FONT_FAMILY = "font-family";
const std::string CSS::Properties::FONT_SIZE = "font-size";
const std::string CSS::Properties::COLOR = "color";
const std::string CSS::Properties::BG_COLOR = "background-color";
const std::string CSS::Properties::MARGIN = "margin";
const std::string CSS::Properties::MARGIN_TOP = "margin-top";
const std::string CSS::Properties::MARGIN_BOTTOM = "margin-bottom";
const std::string CSS::Properties::MARGIN_LEFT = "margin-left";
const std::string CSS::Properties::MARGIN_RIGHT = "margin-right";
const std::string CSS::Properties::PADDING = "padding";
const std::string CSS::Properties::PADDING_TOP = "padding-top";
const std::string CSS::Properties::PADDING_BOTTOM = "padding-bottom";
const std::string CSS::Properties::PADDING_LEFT = "padding-left";
const std::string CSS::Properties::PADDING_RIGHT = "padding-right";
const std::string CSS::Properties::VERTICAL_ALIGN = "vertical-align";
const std::string CSS::Properties::DISPLAY = "display";

std::string CSS::Color::transferColor(const std::string &color) {
    if (!color.empty()) {
        std::string lowerColor = toLowerCase(color);
        auto it = COLOR_MAP.find(lowerColor);
        if (it != COLOR_MAP.end()) {
            return it->second;
        } else if (lowerColor.rfind(COLOR_FLAG, 0) == 0) {
            return color;
        }
        if (std::find(KEYWORD::KEYWORDS.begin(), KEYWORD::KEYWORDS.end(), lowerColor) !=
            KEYWORD::KEYWORDS.end()) {
            return color;
        }
    }
    return "";
}

std::string CSS::Color::toLowerCase(const std::string &str) {
    std::string lowerStr = str;
    std::transform(lowerStr.begin(), lowerStr.end(), lowerStr.begin(), ::tolower);
    return lowerStr;
}

const std::unordered_map<std::string, std::string> CSS::Color::COLOR_MAP = {
        {"aliceblue",            "#F0F8FF"},
        {"antiquewhite",         "#FAEBD7"},
        {"aqua",                 "#00FFFF"},
        {"aquamarine",           "#7FFFD4"},
        {"azure",                "#F0FFFF"},
        {"beige",                "#F5F5DC"},
        {"bisque",               "#FFE4C4"},
        {"black",                "#000000"},
        {"blanchedalmond",       "#FFEBCD"},
        {"blue",                 "#0000FF"},
        {"blueviolet",           "#8A2BE2"},
        {"brown",                "#A52A2A"},
        {"burlywood",            "#DEB887"},
        {"cadetblue",            "#5F9EA0"},
        {"chartreuse",           "#7FFF00"},
        {"chocolate",            "#D2691E"},
        {"coral",                "#FF7F50"},
        {"cornflowerblue",       "#6495ED"},
        {"cornsilk",             "#FFF8DC"},
        {"crimson",              "#DC143C"},
        {"cyan",                 "#00FFFF"},
        {"darkblue",             "#00008B"},
        {"darkcyan",             "#008B8B"},
        {"darkgoldenrod",        "#B8860B"},
        {"darkgray",             "#A9A9A9"},
        {"darkgreen",            "#006400"},
        {"darkkhaki",            "#BDB76B"},
        {"darkmagenta",          "#8B008B"},
        {"darkolivegreen",       "#556B2F"},
        {"darkorange",           "#FF8C00"},
        {"darkorchid",           "#9932CC"},
        {"darkred",              "#8B0000"},
        {"darksalmon",           "#E9967A"},
        {"darkseagreen",         "#8FBC8F"},
        {"darkslateblue",        "#483D8B"},
        {"darkslategray",        "#2F4F4F"},
        {"darkturquoise",        "#00CED1"},
        {"darkviolet",           "#9400D3"},
        {"deeppink",             "#FF1493"},
        {"deepskyblue",          "#00BFFF"},
        {"dimgray",              "#696969"},
        {"dodgerblue",           "#1E90FF"},
        {"feldspar",             "#D19275"},
        {"firebrick",            "#B22222"},
        {"floralwhite",          "#FFFAF0"},
        {"forestgreen",          "#228B22"},
        {"fuchsia",              "#FF00FF"},
        {"gainsboro",            "#DCDCDC"},
        {"ghostwhite",           "#F8F8FF"},
        {"gold",                 "#FFD700"},
        {"goldenrod",            "#DAA520"},
        {"gray",                 "#808080"},
        {"green",                "#008000"},
        {"greenyellow",          "#ADFF2F"},
        {"honeydew",             "#F0FFF0"},
        {"hotpink",              "#FF69B4"},
        {"indianred",            "#CD5C5C"},
        {"indigo",               "#4B0082"},
        {"ivory",                "#FFFFF0"},
        {"khaki",                "#F0E68C"},
        {"lavender",             "#E6E6FA"},
        {"lavenderblush",        "#FFF0F5"},
        {"lawngreen",            "#7CFC00"},
        {"lemonchiffon",         "#FFFACD"},
        {"lightblue",            "#ADD8E6"},
        {"lightcoral",           "#F08080"},
        {"lightcyan",            "#E0FFFF"},
        {"lightgoldenrodyellow", "#FAFAD2"},
        {"lightgrey",            "#D3D3D3"},
        {"lightgreen",           "#90EE90"},
        {"lightpink",            "#FFB6C1"},
        {"lightsalmon",          "#FFA07A"},
        {"lightseagreen",        "#20B2AA"},
        {"lightskyblue",         "#87CEFA"},
        {"lightslateblue",       "#8470FF"},
        {"lightslategray",       "#778899"},
        {"lightsteelblue",       "#B0C4DE"},
        {"lightyellow",          "#FFFFE0"},
        {"lime",                 "#00FF00"},
        {"limegreen",            "#32CD32"},
        {"linen",                "#FAF0E6"},
        {"magenta",              "#FF00FF"},
        {"maroon",               "#800000"},
        {"mediumaquamarine",     "#66CDAA"},
        {"mediumblue",           "#0000CD"},
        {"mediumorchid",         "#BA55D3"},
        {"mediumpurple",         "#9370D8"},
        {"mediumseagreen",       "#3CB371"},
        {"mediumslateblue",      "#7B68EE"},
        {"mediumspringgreen",    "#00FA9A"},
        {"mediumturquoise",      "#48D1CC"},
        {"mediumvioletred",      "#C71585"},
        {"midnightblue",         "#191970"},
        {"mintcream",            "#F5FFFA"},
        {"mistyrose",            "#FFE4E1"},
        {"moccasin",             "#FFE4B5"},
        {"navajowhite",          "#FFDEAD"},
        {"navy",                 "#000080"},
        {"oldlace",              "#FDF5E6"},
        {"olive",                "#808000"},
        {"olivedrab",            "#6B8E23"},
        {"orange",               "#FFA500"},
        {"orangered",            "#FF4500"},
        {"orchid",               "#DA70D6"},
        {"palegoldenrod",        "#EEE8AA"},
        {"palegreen",            "#98FB98"},
        {"paleturquoise",        "#AFEEEE"},
        {"palevioletred",        "#D87093"},
        {"papayawhip",           "#FFEFD5"},
        {"peachpuff",            "#FFDAB9"},
        {"peru",                 "#CD853F"},
        {"pink",                 "#FFC0CB"},
        {"plum",                 "#DDA0DD"},
        {"powderblue",           "#B0E0E6"},
        {"purple",               "#800080"},
        {"red",                  "#FF0000"},
        {"rosybrown",            "#BC8F8F"},
        {"royalblue",            "#4169E1"},
        {"saddlebrown",          "#8B4513"},
        {"salmon",               "#FA8072"},
        {"sandybrown",           "#F4A460"},
        {"seagreen",             "#2E8B57"},
        {"seashell",             "#FFF5EE"},
        {"sienna",               "#A0522D"},
        {"silver",               "#C0C0C0"},
        {"skyblue",              "#87CEEB"},
        {"slateblue",            "#6A5ACD"},
        {"slategray",            "#708090"},
        {"snow",                 "#FFFAFA"},
        {"springgreen",          "#00FF7F"},
        {"steelblue",            "#4682B4"},
        {"tan",                  "#D2B48C"},
        {"teal",                 "#008080"},
        {"thistle",              "#D8BFD8"},
        {"tomato",               "#FF6347"},
        {"turquoise",            "#40E0D0"},
        {"violet",               "#EE82EE"},
        {"violetred",            "#D02090"},
        {"wheat",                "#F5DEB3"},
        {"white",                "#FFFFFF"},
        {"whitesmoke",           "#F5F5F5"},
        {"yellow",               "#FFFF00"},
        {"yellowgreen",          "#9ACD32"}
};

const std::string CSS::Color::COLOR_FLAG = "#";

const std::string CSS::Color::KEYWORD::INITIAL = "initial";
const std::string CSS::Color::KEYWORD::INHERIT = "inherit";
const std::string CSS::Color::KEYWORD::UNSET = "unset";
const std::string CSS::Color::KEYWORD::REVERT = "revert";
const std::vector<std::string> CSS::Color::KEYWORD::KEYWORDS = {
        CSS::Color::KEYWORD::INITIAL,
        CSS::Color::KEYWORD::INHERIT,
        CSS::Color::KEYWORD::UNSET,
        CSS::Color::KEYWORD::REVERT
};