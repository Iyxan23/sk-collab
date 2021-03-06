package com.iyxan23.sketch.collab.helpers;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import java.util.regex.Pattern;

public class SyntaxHighlightingHelper {

    // TODO: MAKE THIS CUSTOMIZEABLE IN SETTINGS ACTIVITY (SOON)

    public static SpannableString highlight_patch(String data) {
        SpannableString spannable = new SpannableString(data);

        // Highlight the @@ thing
        SpannableHelper.span_regex(0xFFD55FDE, spannable, "@@");

        // Highlight the +
        SpannableHelper.span_regex(0xFF89CA78, spannable, "^\\+.+", Pattern.MULTILINE);

        // Highlight the -
        SpannableHelper.span_regex(0xFFEF596F, spannable, "^-.+", Pattern.MULTILINE);

        return spannable;
    }

    public static SpannableString highlight_logic(String data) {
        SpannableString spannable = new SpannableString(data);

        // Highlight the title's activity name
        SpannableHelper.span_regex(0xFFEF596F, spannable, "^@\\w+", Pattern.MULTILINE);

        // Highlight the title's type
        SpannableHelper.span_regex(0xFFB856D8, spannable, "\\.java_.+");

        // Highlight the variable types
        SpannableHelper.span_regex(0xFFD19A66, spannable, "^\\w+:", Pattern.MULTILINE);

        // Highlight the variable names
        SpannableHelper.span_regex(0xFFB59448, spannable, ":\\w+$", Pattern.MULTILINE);

        highlight_json(spannable);

        // Highlight the %s, %m.view thingy
        SpannableHelper.span_regex(0xFFE5C07B, spannable, "%[a-z]\\.(\\w+)|%[a-z]");

        return spannable;
    }

    public static SpannableString highlight_view(String data) {
        SpannableString spannable = new SpannableString(data);

        // Highlight the @activity
        SpannableHelper.span_regex(0xFFEF596F, spannable, "^@\\w+", Pattern.MULTILINE);

        // Highlight the .xml_thing
        SpannableHelper.span_regex(0xFFB856D8, spannable, "\\.xml_.+|\\.xml");

        highlight_json(spannable);

        return spannable;
    }

    public static SpannableString highlight_file(String data) {
        SpannableString spannable = new SpannableString(data);

        // Highlight the @activity
        SpannableHelper.span_regex(0xFFEF596F, spannable, "^@activity", Pattern.MULTILINE);

        // Highlight the @customview
        SpannableHelper.span_regex(0xFFB856D8, spannable, "^@customview", Pattern.MULTILINE);

        highlight_json(spannable);

        return spannable;
    }

    public static SpannableString highlight_library(String data) {
        SpannableString spannable = new SpannableString(data);

        // Highlight the @stuff
        SpannableHelper.span_regex(0xFFEF596F, spannable, "^@\\w+", Pattern.MULTILINE);

        highlight_json(spannable);

        return spannable;
    }

    public static SpannableString highlight_resource(String data) {
        // Basically the same as library, but putting this with a different function name will make
        // the code to be more consistent / no weird functions
        return highlight_library(data);
    }

    public static void highlight_json(SpannableString data) {
        // Highlight the numbers
        SpannableHelper.span_regex(0xFFD19A66, data, "\\d+|-\\d+|\\.\\d*");

        // Highlight the string in json
        SpannableHelper.span_regex(0xFF89CA78, data, "(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")");
    }
}
