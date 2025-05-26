package com.escape.urmanager.util

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

object SpannableHelper {

    fun colorText(color: Int, text: String): SpannableString {
        val spannable = SpannableString(text)

        spannable.setSpan(ForegroundColorSpan(color), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(RelativeSizeSpan(1.1f), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannable
    }
}