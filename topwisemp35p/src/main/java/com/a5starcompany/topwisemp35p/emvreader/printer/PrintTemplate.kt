package com.a5starcompany.topwisemp35p.emvreader.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.a5starcompany.topwisemp35p_horizonpay.printer.Align
import com.a5starcompany.topwisemp35p_horizonpay.printer.ImageUnit
import com.a5starcompany.topwisemp35p_horizonpay.printer.TextUnit
import com.horizonpay.utils.BaseUtils
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PrintTemplate {
    private var hasInit = false
    private var root: LinearLayout? = null
    private var context: Context? = null
    private var typeface: Typeface? = null
    private var templateBitmap: Bitmap? = null

    // Background thread for heavy operations
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context?, typeface: Typeface?) {
        if (!hasInit) {
            this.context = context
            this.root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(384, -2)
            }

            this.typeface = typeface ?: try {
//                ResourcesCompat.getFont(context!!, R.font.satoshi_medium)
                Typeface.createFromAsset(BaseUtils.getApp().assets, "fonts/satoshi_medium.ttf")
            } catch (e: Exception) {
                Log.e("PrintTemplate", "Font loading failed", e)
                Typeface.DEFAULT
            }

            hasInit = true
        }
    }

    fun setTypeface(typeface: Typeface?) {
        this.typeface = typeface
    }

    fun add(textUnit: TextUnit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                root?.addView(getPrnTextView(textUnit, 384))
            }
        } else {
            root?.addView(getPrnTextView(textUnit, 384))
        }
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
        get() = runCatching {
            val h = measureHeight(root!!)
            root!!.layout(0, 0, 384, h)

            templateBitmap?.recycle()
            Bitmap.createBitmap(384, h, Bitmap.Config.ARGB_8888).apply {
                setHasAlpha(true)
                Canvas(this).apply {
                    drawColor(Color.WHITE)
                    root!!.draw(this)
                }
                templateBitmap = this
            }
        }.onFailure {
            Log.e("PrintTemplate", "Bitmap creation failed", it)
        }.getOrNull()


    // Async version (recommended)
    fun getPrintBitmapAsync(callback: (Bitmap?) -> Unit) {
        backgroundExecutor.execute {
            try {
                // Get measurements on main thread
                mainHandler.post {
                    val h = measureHeight(root!!)
                    root!!.layout(0, 0, 384, h)

                    // Move bitmap creation back to background
                    backgroundExecutor.execute {
                        try {
                            templateBitmap?.let {
                                if (!it.isRecycled) it.recycle()
                            }

                            val bitmap = Bitmap.createBitmap(384, h, Bitmap.Config.ARGB_8888).apply {
                                setHasAlpha(true)
                            }

                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)

                            // Draw view on main thread
                            mainHandler.post {
                                try {
                                    root!!.draw(canvas)
                                    templateBitmap = bitmap
                                    callback(bitmap)
                                } catch (e: Exception) {
                                    Log.e("PrintTemplate", "Drawing failed", e)
                                    callback(null)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PrintTemplate", "Background bitmap creation failed", e)
                            mainHandler.post { callback(null) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PrintTemplate", "Measurement failed", e)
                mainHandler.post { callback(null) }
            }
        }
    }

    fun clear() {
        // Clear on background thread to avoid blocking UI
        backgroundExecutor.execute {
            templateBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
                templateBitmap = null
            }

            // Clear views on main thread
            mainHandler.post {
                unbindResource(this.root)
            }
        }
    }

    // Non-blocking clear version
    fun clearAsync(callback: (() -> Unit)? = null) {
        backgroundExecutor.execute {
            templateBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
                templateBitmap = null
            }

            // Clear views on main thread
            mainHandler.post {
                unbindResource(this.root)
                callback?.invoke()
            }
        }
    }

    private fun unbindResource(view: View?) {
        if (view == null) {
            Log.i("PrintTemplate", "view == null")
            return
        }

        Log.i("PrintTemplate", "unbindResource")

        when (view) {
            is ImageView -> {
                Log.i("PrintTemplate", "Processing ImageView")
                view.drawable?.let { drawable ->
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap?.let { bitmap ->
                            if (!bitmap.isRecycled) {
                                // Move bitmap recycling to background thread
                                backgroundExecutor.execute {
                                    bitmap.recycle()
                                }
                            }
                        }
                    }
                }
                // Clear the drawable reference immediately
                view.setImageDrawable(null)
            }
            is ViewGroup -> {
                Log.i("PrintTemplate", "Processing ViewGroup with ${view.childCount} children")
                // Process children first
                for (i in 0 until view.childCount) {
                    unbindResource(view.getChildAt(i))
                }
                // Remove all views in batches to reduce UI thread work
                view.removeAllViews()
            }
        }
    }

    private fun measureHeight(child: View): Int {
        child.measure(
            View.MeasureSpec.makeMeasureSpec(384, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return child.measuredHeight
    }

    private fun getPrnTextView(textUnit: TextUnit, width: Int): TextView {
        return TextView(context).apply {
            val tvParams = LinearLayout.LayoutParams(width, -2)
            tvParams.setMargins(0, textUnit.lineSpacing / 2, 0, textUnit.lineSpacing / 2)
            layoutParams = tvParams

            gravity = when (textUnit.align) {
                Align.CENTER -> Gravity.CENTER
                Align.RIGHT -> Gravity.RIGHT
                else -> Gravity.LEFT
            }

            isSingleLine = !textUnit.isWordWrap

            paint.flags = if (textUnit.isUnderline) {
                Paint.ANTI_ALIAS_FLAG or Paint.UNDERLINE_TEXT_FLAG
            } else {
                Paint.ANTI_ALIAS_FLAG
            }

            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = textUnit.strokeWidth
            setTextColor(Color.BLACK)

            val style = if (textUnit.isBold) Typeface.BOLD else Typeface.NORMAL

            when {
                textUnit.fontType != null -> setTypeface(textUnit.fontType, style)
                this@PrintTemplate.typeface != null -> setTypeface(this@PrintTemplate.typeface, style)
                else -> typeface = Typeface.defaultFromStyle(style)
            }

            setTextSize(TypedValue.COMPLEX_UNIT_PX, textUnit.fontSize.toFloat())
            letterSpacing = textUnit.letterSpacing.toFloat() / textUnit.fontSize.toFloat()
            text = textUnit.text
        }
    }

    fun shutdown() {
        backgroundExecutor.shutdown()
        try {
            if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            backgroundExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        private const val TAG = "PrintTemplate"
        val instance = PrintTemplate()
        private const val PAPER_WIDTH = 384
    }
}