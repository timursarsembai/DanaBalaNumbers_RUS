package com.timursarsembayev.danabalanumbers

import android.app.Activity
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Централизованное управление баннером AdMob для экрана.
 */
class BannerManager(
    private val adsEnabledProvider: () -> Boolean,
    private val onLog: (String) -> Unit = {}
) {
    private var adView: AdView? = null
    private var container: ViewGroup? = null
    private var activity: Activity? = null
    private var adUnitId: String? = null
    private var lastWidthDp: Int = 0

    fun attach(activity: Activity, container: ViewGroup, adUnitId: String) {
        this.activity = activity
        this.container = container
        this.adUnitId = adUnitId
        if (!adsEnabledProvider()) {
            container.visibility = View.GONE
            onLog("Ads disabled -> hiding container")
            return
        }
        container.visibility = View.VISIBLE
        waitForWidthAndLoad()
    }

    fun onResume() {
        adView?.resume()
    }

    fun onPause() {
        adView?.pause()
    }

    fun onDestroy() {
        adView?.destroy()
        adView = null
        container = null
        activity = null
    }

    fun reloadIfWidthChanged() {
        val act = activity ?: return
        val cont = container ?: return
        if (cont.width <= 0) return
        val widthDp = pxToDp(act, cont.width)
        if (widthDp != lastWidthDp) {
            onLog("Width changed $lastWidthDp -> $widthDp, reloading banner")
            loadBanner()
        }
    }

    private fun waitForWidthAndLoad() {
        val cont = container ?: return
        if (cont.width > 0) {
            loadBanner()
            return
        }
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (cont.width > 0) {
                    cont.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    loadBanner()
                }
            }
        }
        cont.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun loadBanner() {
        val act = activity ?: return
        val cont = container ?: return
        val id = adUnitId ?: return
        if (!adsEnabledProvider()) {
            cont.visibility = View.GONE
            return
        }
        cont.visibility = View.VISIBLE

        // Рассчитать адаптивный размер под текущую ширину контейнера
        val widthDp = pxToDp(act, cont.width)
        if (widthDp <= 0) return
        lastWidthDp = widthDp
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(act, widthDp)

        // Пересоздаём AdView под новый размер
        adView?.destroy()
        adView = AdView(act).apply {
            adUnitId = id
            setAdSize(adSize)
        }
        cont.removeAllViews()
        cont.addView(adView)

        val request = AdRequest.Builder().build()
        onLog("Loading banner (widthDp=$widthDp, size=$adSize)")
        adView?.loadAd(request)
    }

    private fun pxToDp(activity: Activity, px: Int): Int {
        val dm: DisplayMetrics = activity.resources.displayMetrics
        return (px / (dm.density)).toInt().coerceAtLeast(0)
    }
}

