package com.mccal.folio

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Offset

internal class ZeroPaddingWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(context: Context, appWidgetId: Int,
        appWidget: AppWidgetProviderInfo): AppWidgetHostView =
        ZeroPaddingWidgetHostView(context)
}

internal class ZeroPaddingWidgetHostView(context: Context) : AppWidgetHostView(context) {
    private var providerOwnsVerticalGesture = false
    private var stretch = CardStretch.NONE
    var fillsBox = false
        private set
    var onFillsBoxChanged: ((Boolean) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(fillsBox)
        }
    private val measureFill = Runnable {
        if (!isAttachedToWindow || width <= 0 || height <= 0) return@Runnable
        val fills = drawnFillsBox()
        if (!fills && stretchWrappedCard()) return@Runnable
        if (fills != fillsBox) {
            fillsBox = fills
            onFillsBoxChanged?.invoke(fills)
        }
    }
    private val publishCurrentSize = Runnable {
        if (!isAttachedToWindow || appWidgetId < 0 || width <= 0 || height <= 0) return@Runnable
        val density = resources.displayMetrics.density
        publishExactWidgetSize(this, width / density, height / density)
    }

    override fun setAppWidget(appWidgetId: Int, info: AppWidgetProviderInfo?) {
        if (appWidgetId != this.appWidgetId) stretch = CardStretch.NONE
        super.setAppWidget(appWidgetId, info)
        setPadding(0, 0, 0, 0)
        scheduleSizePublication()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleSizePublication()
    }

    override fun onSizeChanged(widthPx: Int, heightPx: Int, oldWidthPx: Int, oldHeightPx: Int) {
        super.onSizeChanged(widthPx, heightPx, oldWidthPx, oldHeightPx)
        if (appWidgetId < 0 || widthPx <= 0 || heightPx <= 0) return
        // Avoid provider IPC inside a View/Compose layout callback. Coalesce size publication
        // until after the current layout pass finishes.
        scheduleSizePublication()
    }

    override fun updateAppWidget(remoteViews: android.widget.RemoteViews?) {
        super.updateAppWidget(remoteViews)
        scheduleFillCheck()
    }

    override fun prepareView(view: View) {
        super.prepareView(view)
        view.layoutParams?.let(::stretchToSlot)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        scheduleFillCheck()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(measureFill)
        if (providerOwnsVerticalGesture) parent?.requestDisallowInterceptTouchEvent(false)
        providerOwnsVerticalGesture = false
        removeCallbacks(publishCurrentSize)
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Compose's Home column is vertically scrollable too. Give a provider's
                // ListView/ScrollView the uninterrupted native stream when DOWN hit that
                // content. The common pager observes Initial events before AndroidView, so
                // a horizontal swipe can still claim its axis and cancel this stream.
                providerOwnsVerticalGesture = nativeWidgetConsumesVerticalGesture(
                    this,
                    Offset(event.rawX, event.rawY),
                )
                if (providerOwnsVerticalGesture) parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (providerOwnsVerticalGesture) parent?.requestDisallowInterceptTouchEvent(false)
                providerOwnsVerticalGesture = false
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun scheduleFillCheck() {
        removeCallbacks(measureFill)
        postDelayed(measureFill, FILL_CHECK_DELAY_MS)
    }

    private fun stretchWrappedCard(): Boolean {
        val card = getChildAt(0) ?: return false
        val params = card.layoutParams ?: return false
        val next = wrappedCardStretch(
            slotWidth = width, slotHeight = height,
            cardWidth = card.width, cardHeight = card.height,
            wrapsWidth = params.width == ViewGroup.LayoutParams.WRAP_CONTENT,
            wrapsHeight = params.height == ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        if (next == CardStretch.NONE || !card.drawnFillsBox()) return false
        stretch = CardStretch(width = stretch.width || next.width, height = stretch.height || next.height)
        stretchToSlot(params)
        card.layoutParams = params
        return true
    }

    private fun stretchToSlot(params: ViewGroup.LayoutParams) {
        if (stretch.width && params.width == ViewGroup.LayoutParams.WRAP_CONTENT) params.width = ViewGroup.LayoutParams.MATCH_PARENT
        if (stretch.height && params.height == ViewGroup.LayoutParams.WRAP_CONTENT) params.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun scheduleSizePublication() {
        removeCallbacks(publishCurrentSize)
        post(publishCurrentSize)
    }
}

private const val FILL_CHECK_DELAY_MS = 250L
private const val FILL_SAMPLE_PX = 96f
private const val STRETCHABLE_CARD_SHARE = .75f

private fun View.drawnFillsBox(): Boolean = runCatching {
    if (width <= 0 || height <= 0) return@runCatching false
    val scale = FILL_SAMPLE_PX / maxOf(width, height).toFloat()
    val sampleWidth = maxOf(1, (width * scale).toInt())
    val sampleHeight = maxOf(1, (height * scale).toInt())
    val sample = android.graphics.Bitmap.createBitmap(sampleWidth, sampleHeight, android.graphics.Bitmap.Config.ARGB_8888)
    try {
        val canvas = android.graphics.Canvas(sample)
        canvas.scale(scale, scale)
        draw(canvas)
        fillsBox(sampleWidth, sampleHeight) { x, y -> android.graphics.Color.alpha(sample.getPixel(x, y)) }
    } finally {
        sample.recycle()
    }
}.getOrDefault(false)

internal data class CardStretch(val width: Boolean, val height: Boolean) {
    companion object {
        val NONE = CardStretch(width = false, height = false)
    }
}

internal fun wrappedCardStretch(
    slotWidth: Int, slotHeight: Int,
    cardWidth: Int, cardHeight: Int,
    wrapsWidth: Boolean, wrapsHeight: Boolean,
): CardStretch {
    fun stretchable(card: Int, slot: Int) = card < slot && card >= slot * STRETCHABLE_CARD_SHARE
    return CardStretch(
        width = wrapsWidth && cardHeight >= slotHeight && stretchable(cardWidth, slotWidth),
        height = wrapsHeight && cardWidth >= slotWidth && stretchable(cardHeight, slotHeight),
    )
}

internal fun exactWidgetSizeOptions(widthDp: Float, heightDp: Float) = Bundle().apply {
    val width = widthDp.coerceAtLeast(1f)
    val height = heightDp.coerceAtLeast(1f)
    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width.toInt())
    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width.toInt())
    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height.toInt())
    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height.toInt())
    putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(SizeF(width, height)))
}

private fun publishExactWidgetSize(view: AppWidgetHostView, widthDp: Float, heightDp: Float) {
    val options = exactWidgetSizeOptions(widthDp, heightDp)
    val current = AppWidgetManager.getInstance(view.context).getAppWidgetOptions(view.appWidgetId)
    val sameBounds = listOf(
        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
    ).all { current.getInt(it) == options.getInt(it) }
    val currentSizes = current.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
    val nextSizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
    if (!sameBounds || currentSizes != nextSizes) view.updateAppWidgetOptions(options)
}
