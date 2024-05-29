package com.a5starcompany.topwisemp35p.emvreader.printer

import android.graphics.Typeface


class TextUnit {
    var text: String? = null
    var fontSize = 22
    var isBold = false
        private set
    var fontType: Typeface? = null
        private set
    var align: Align
    var isUnderline: Boolean
        private set
    var isWordWrap: Boolean
        private set
    var lineSpacing: Int
        private set
    var letterSpacing: Int
        private set

    constructor(text: String?) {
        align = Align.LEFT
        isUnderline = false
        isWordWrap = true
        lineSpacing = 0
        letterSpacing = 0
        this.text = text
    }

    constructor(text: String?, align: Align) {
        this.align = Align.LEFT
        isUnderline = false
        isWordWrap = true
        lineSpacing = 0
        letterSpacing = 0
        this.text = text
        this.align = align
    }

    constructor(text: String?, fontSize: Int) {
        align = Align.LEFT
        isUnderline = false
        isWordWrap = true
        lineSpacing = 0
        letterSpacing = 0
        this.text = text
        this.fontSize = fontSize
    }

    constructor(text: String?, fontSize: Int, align: Align) {
        this.align = Align.LEFT
        isUnderline = false
        isWordWrap = true
        lineSpacing = 0
        letterSpacing = 0
        this.text = text
        this.fontSize = fontSize
        this.align = align
    }

    fun setFontType(fontType: Typeface?): TextUnit {
        this.fontType = fontType
        return this
    }

    fun setLetterSpacing(letterSpacing: Int): TextUnit {
        this.letterSpacing = letterSpacing
        return this
    }

    fun setLineSpacing(lineSpacing: Int): TextUnit {
        this.lineSpacing = lineSpacing
        return this
    }

    fun setBold(bold: Boolean): TextUnit {
        isBold = bold
        return this
    }

    fun setUnderline(underline: Boolean): TextUnit {
        isUnderline = underline
        return this
    }

    fun setWordWrap(wordWrap: Boolean): TextUnit {
        isWordWrap = wordWrap
        return this
    }

    fun copyValue(unit: TextUnit) {
        text = unit.text
        align = unit.align
        fontSize = unit.fontSize
        fontType = unit.fontType
        isBold = unit.isBold
        isUnderline = unit.isUnderline
        isWordWrap = unit.isWordWrap
        lineSpacing = unit.lineSpacing
        letterSpacing = unit.letterSpacing
    }

    object TextSize {
        const val SMALL = 16
        const val NORMAL = 22
        const val LARGE = 32
        const val XLARGE = 48
    }
}