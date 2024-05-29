package com.a5starcompany.topwisemp35p.emvreader.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.a5starcompany.topwisemp35p.R

class PrintTemplate {
    private var hasInit = false
    private var root: LinearLayout? = null
    private var context: Context? = null
    private var typeface: Typeface? = null
    var strokeWidth = 0.0f
    private var templateBitmap: Bitmap? = null

    fun init(context: Context?, typeface: Typeface?) {
        if (!hasInit) {
            this.context = context
            this.root = LinearLayout(context)
            this.root!!.orientation = LinearLayout.VERTICAL
            val layoutParams = LinearLayout.LayoutParams(384, -2)
            this.root!!.layoutParams = layoutParams
            try {
                if (typeface == null) {
//                    this.typeface = Typeface.createFromFile("/system/fonts/NSimSun.ttf")
                    this.typeface = ResourcesCompat.getFont(this.context!!, R.font.satoshi_medium)

                } else {
                    this.typeface = typeface
                }
            } catch (var5: Exception) {
                Log.d("PrintTemplate", "font is not exist!")
                var5.printStackTrace()
            }
            hasInit = true
        }
    }

    fun setTypeface(typeface: Typeface?) {
        this.typeface = typeface
    }

    fun add(textUnit: TextUnit) {
        this.root!!.addView(getPrnTextView(textUnit, 384))
    }
    fun add(weight1: Int, textUnit1: TextUnit, weight2: Int, textUnit2: TextUnit) {
        val layout = LinearLayout(context)
        val params = LinearLayout.LayoutParams(384, -2)
        layout.layoutParams = params
        layout.orientation = LinearLayout.HORIZONTAL
        val width1 = 384 * weight1 / (weight1 + weight2)
        layout.addView(getPrnTextView(textUnit1, width1))
        val width2 = 384 - width1
        layout.addView(getPrnTextView(textUnit2, width2))
        this.root!!.addView(layout)
    }

    fun add(
        weight1: Int,
        textUnit1: TextUnit,
        weight2: Int,
        textUnit2: TextUnit,
        weight3: Int,
        textUnit3: TextUnit
    ) {
        val layout = LinearLayout(context)
        val params = LinearLayout.LayoutParams(384, -2)
        layout.layoutParams = params
        layout.orientation = LinearLayout.HORIZONTAL
        val width1 = 384 * weight1 / (weight1 + weight2 + weight3)
        layout.addView(getPrnTextView(textUnit1, width1))
        val width2 = 384 * weight2 / (weight1 + weight2 + weight3)
        layout.addView(getPrnTextView(textUnit2, width2))
        val width3 = 384 - width1 - width2
        layout.addView(getPrnTextView(textUnit3, width3))
        this.root!!.addView(layout)
    }

fun add(
    weight1: Int,
    textUnit1: TextUnit,
    weight2: Int,
    textUnit2: TextUnit,
    weight3: Int,
    textUnit3: TextUnit,
    weight4: Int,
    textUnit4: TextUnit
    ) {
        val layout = LinearLayout(context)
        val params = LinearLayout.LayoutParams(384, -2)
        layout.layoutParams = params
        layout.orientation = LinearLayout.HORIZONTAL
        val width1 = 384 * weight1 / (weight1 + weight2 + weight3 + weight4)
        layout.addView(getPrnTextView(textUnit1, width1))
        val width2 = 384 * weight2 / (weight1 + weight2 + weight3 + weight4)
        layout.addView(getPrnTextView(textUnit2, width2))
        val width3 = 384 * weight3 / (weight1 + weight2 + weight3 + weight4)
        layout.addView(getPrnTextView(textUnit3, width3))
        val width4 = 384 - width1 - width2 - width3
        layout.addView(getPrnTextView(textUnit4, width4))
        this.root!!.addView(layout)
    }

//    fun add(
//        textUnit1: TextUnit
//    ) {
//        val layout = LinearLayout(context)
//        val params = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
//        params.gravity = Gravity.CENTER
//        layout.layoutParams = params
//        layout.orientation = LinearLayout.HORIZONTAL
//        layout.addView(getPrnTextView(textUnit1, 384))
//        this.root!!.addView(layout)
//    }

    fun add(imageUnit: ImageUnit) {
        val imageView = ImageView(context)
        val margins = imageUnit.margins
        val imageParams = LinearLayout.LayoutParams(imageUnit.width, imageUnit.height)
        if (imageUnit.align == Align.CENTER) {
            imageParams.gravity = 1
        } else if (imageUnit.align == Align.RIGHT) {
            imageParams.gravity = 5
        } else {
            imageParams.gravity = 3
        }
        imageParams.setMargins(margins.left, margins.top, margins.right, margins.bottom)
        imageView.layoutParams = imageParams
        imageView.setImageBitmap(imageUnit.image)
        this.root!!.addView(imageView)
    }

    fun add(textList: List<TextUnit?>, imageUnit: ImageUnit) {
        val layout = LinearLayout(context)
        val params = LinearLayout.LayoutParams(384, -2)
        layout.layoutParams = params
        layout.orientation = LinearLayout.HORIZONTAL
        val imageView = ImageView(context)
        val margins = imageUnit.margins
        val imageParams = LinearLayout.LayoutParams(imageUnit.width, imageUnit.height)
        imageParams.setMargins(margins.left, margins.top, margins.right, margins.bottom)
        imageView.layoutParams = imageParams
        imageView.setImageBitmap(imageUnit.image)
        val layout1 = LinearLayout(context)
        val width1 = 384 - (imageUnit.width + margins.left + margins.right)
        val params1 = LinearLayout.LayoutParams(width1, -2)
        layout1.layoutParams = params1
        layout1.orientation = LinearLayout.VERTICAL
        val var11: Iterator<*> = textList.iterator()
        while (var11.hasNext()) {
            val textUnit = var11.next() as TextUnit
            layout1.addView(getPrnTextView(textUnit, width1))
        }
        layout.addView(layout1)
        layout.addView(imageView)
        this.root!!.addView(layout)
    }

    fun add(imageUnit: ImageUnit, textList: List<TextUnit?>) {
        val layout = LinearLayout(context)
        val params = LinearLayout.LayoutParams(384, -2)
        layout.layoutParams = params
        layout.orientation = LinearLayout.HORIZONTAL
        val imageView = ImageView(context)
        val margins = imageUnit.margins
        val imageParams = LinearLayout.LayoutParams(imageUnit.width, imageUnit.height)
        imageParams.setMargins(margins.left, margins.top, margins.right, margins.bottom)
        imageView.layoutParams = imageParams
        imageView.setImageBitmap(imageUnit.image)
        val layout1 = LinearLayout(context)
        val width1 = 384 - (imageUnit.width + margins.left + margins.right)
        val params1 = LinearLayout.LayoutParams(width1, -2)
        layout1.layoutParams = params1
        layout1.orientation = LinearLayout.VERTICAL
        val var11: Iterator<*> = textList.iterator()
        while (var11.hasNext()) {
            val textUnit = var11.next() as TextUnit
            layout1.addView(getPrnTextView(textUnit, width1))
        }
        layout.addView(imageView)
        layout.addView(layout1)
        this.root!!.addView(layout)
    }

    val printBitmap: Bitmap?
        get() {
            this.root!!.isDrawingCacheEnabled = true
            val h = measureHeight(this.root)
            this.root!!.layout(0, 0, 384, h)
            if (templateBitmap != null) {
                templateBitmap!!.recycle()
                templateBitmap = null
            }
            templateBitmap = Bitmap.createBitmap(384, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(templateBitmap!!)
            canvas.drawColor(-1)
            this.root!!.draw(canvas)
            return templateBitmap
        }

    fun clear() {
        if (templateBitmap != null) {
            templateBitmap!!.recycle()
            templateBitmap = null
        }
        unbindResource(this.root)
    }

    private fun unbindResource(view: View?) {
        if (view == null) {
            Log.i("PrintTemplate", "view == null")
        } else {
            Log.i("PrintTemplate", "unbindResource")
            if (view is ImageView) {
                Log.i("PrintTemplate", "recycle")
                val drawable = view.drawable as BitmapDrawable
                var bitmap = drawable.bitmap
                if (bitmap != null) {
                    bitmap.recycle()
                    bitmap = null
                }
            }
            if (view is ViewGroup) {
                Log.i("PrintTemplate", "ViewGroup")
                for (i in 0 until view.childCount) {
                    unbindResource(view.getChildAt(i))
                }
                view.removeAllViews()
            }
        }
    }

    private fun measureHeight(child: View?): Int {
        var lp = child!!.layoutParams
        if (lp == null) {
            lp = ViewGroup.LayoutParams(-1, -2)
        }
        val childMeasureWidth = ViewGroup.getChildMeasureSpec(0, 0, lp.width)
        val childMeasureHeight: Int
        childMeasureHeight = if (lp.height > 0) {
            View.MeasureSpec.makeMeasureSpec(lp.height, View.MeasureSpec.EXACTLY)
        } else {
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        }
        child.measure(childMeasureWidth, childMeasureHeight)
        return child.measuredHeight
    }


    private fun getPrnTextView(textUnit: TextUnit, width: Int): TextView {
        val tv = TextView(context)
        val tvParams = LinearLayout.LayoutParams(width, -2)
        tvParams.setMargins(0, textUnit.lineSpacing / 2, 0, textUnit.lineSpacing / 2)
        tv.layoutParams = tvParams
        if (textUnit.align == Align.CENTER) {
            tv.gravity = Gravity.CENTER
        } else if (textUnit.align == Align.RIGHT) {
            tv.gravity = Gravity.RIGHT
        } else {
            tv.gravity = Gravity.LEFT
        }
        if (!textUnit.isWordWrap) {
            tv.isSingleLine = true
        }
        if (textUnit.isUnderline) {
            tv.paint.flags = 9
        } else {
            tv.paint.flags = Paint.ANTI_ALIAS_FLAG
        }
        tv.paint.isAntiAlias = true
        tv.paint.style = Paint.Style.FILL_AND_STROKE
        tv.paint.strokeWidth = strokeWidth
        tv.setTextColor(-16777216)
        var style = Typeface.BOLD
        if (textUnit.isBold) {
            style = Typeface.BOLD
        }
        if (textUnit.fontType != null) {
            tv.setTypeface(textUnit.fontType, style)
        } else if (typeface != null) {
            tv.setTypeface(typeface, style)
        } else {
            tv.typeface = Typeface.defaultFromStyle(style)
        }
        tv.setTextSize(0, textUnit.fontSize.toFloat())
        tv.letterSpacing = textUnit.letterSpacing.toFloat() / textUnit.fontSize.toFloat()
        tv.text = textUnit.text
        return tv
    }

    companion object {
        private const val TAG = "PrintTemplate"
        val instance = PrintTemplate()
        private const val PAPER_WIDTH = 384
    }
}