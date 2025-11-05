package com.timursarsembayev.danabalanumbers

import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

/**
 * Базовая активити, которая умеет показывать нижний баннер через BannerManager.
 */
abstract class BaseActivity : AppCompatActivity() {
    protected open val adUnitId: String? = null

    private val bannerManager by lazy {
        BannerManager(
            adsEnabledProvider = { !(application as DanaBalApplication).billing.isPremium() },
            onLog = { /* no-op or Log.d */ }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Вызывайте после setContentView в наследниках, либо используйте setContentViewWithBanner.
     */
    protected fun attachBannerIfPossible(container: ViewGroup?) {
        val id = adUnitId
        if (id != null && container != null) {
            bannerManager.attach(this, container, id)
        }
    }

    protected fun setContentViewWithBanner(@LayoutRes layoutRes: Int, container: ViewGroup?) {
        setContentView(layoutRes)
        attachBannerIfPossible(container)
    }

    override fun onResume() {
        super.onResume()
        bannerManager.onResume()
        bannerManager.reloadIfWidthChanged()
    }

    override fun onPause() {
        bannerManager.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        bannerManager.onDestroy()
        super.onDestroy()
    }
}

