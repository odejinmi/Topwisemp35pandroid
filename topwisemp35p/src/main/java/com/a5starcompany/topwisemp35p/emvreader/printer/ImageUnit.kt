package com.a5starcompany.topwisemp35p.emvreader.printer

import android.graphics.Bitmap
import android.graphics.Rect


class ImageUnit {
    var margins = Rect(0, 0, 0, 0)
        private set
    var width = 0
    var height = 0
    var align: Align
    var image: Bitmap?

    constructor(image: Bitmap?, w: Int, h: Int) {
        align = Align.LEFT
        this.image = null
        this.image = image
        width = w
        height = h
    }

    constructor(margins: Rect, image: Bitmap?, w: Int, h: Int) {
        align = Align.LEFT
        this.image = null
        this.margins = margins
        this.image = image
        width = w
        height = h
    }

    constructor(align: Align, image: Bitmap?, w: Int, h: Int) {
        this.align = Align.LEFT
        this.image = null
        this.align = align
        this.image = image
        width = w
        height = h
    }

    fun copyValue(unit: ImageUnit) {
        margins = unit.margins
        align = unit.align
        image = unit.image
        height = unit.height
        width = unit.width
    }
}