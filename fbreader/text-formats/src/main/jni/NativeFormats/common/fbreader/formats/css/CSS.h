//
// Created by JiangXuehao on 2024/11/14.
//
#include <string>
#include <iostream>
#include <unordered_map>
#include <vector>
#include <algorithm>

#ifndef __READER_CSS_H__
#define __READER_CSS_H__
namespace CSS {
    class Properties {
    public:
        static const std::string PAGE_BREAK_BEFORE;
        static const std::string PAGE_BREAK_AFTER;
        static const std::string TEXT_ALIGN;
        static const std::string TEXT_DECORATION;
        static const std::string TEXT_INDENT;
        static const std::string FONT_WEIGHT;
        static const std::string FONT_STYLE;
        static const std::string FONT_VARIANT;
        static const std::string FONT_FAMILY;
        static const std::string FONT_SIZE;
        static const std::string COLOR;
        static const std::string BG_COLOR;
        static const std::string MARGIN;
        static const std::string MARGIN_TOP;
        static const std::string MARGIN_BOTTOM;
        static const std::string MARGIN_LEFT;
        static const std::string MARGIN_RIGHT;
        static const std::string PADDING;
        static const std::string PADDING_TOP;
        static const std::string PADDING_BOTTOM;
        static const std::string PADDING_LEFT;
        static const std::string PADDING_RIGHT;
        static const std::string VERTICAL_ALIGN;
        static const std::string DISPLAY;
    };

    class Color {
    public:
        static const std::unordered_map<std::string, std::string> COLOR_MAP;
        static const std::string COLOR_FLAG;

        struct KEYWORD {
            static const std::string INITIAL;
            static const std::string INHERIT;
            static const std::string UNSET;
            static const std::string REVERT;
            static const std::vector<std::string> KEYWORDS;
        };

    public:
        static std::string transferColor(const std::string& color);
    private:
        static std::string toLowerCase(const std::string& str);
    };
}
#endif //__READER_CSS_H__
