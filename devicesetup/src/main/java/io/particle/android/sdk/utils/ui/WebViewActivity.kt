package io.particle.android.sdk.utils.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.webkit.WebView
import android.webkit.WebViewClient
import io.particle.android.sdk.devicesetup.R
import io.particle.android.sdk.utils.SEGAnalytics


class WebViewActivity : AppCompatActivity() {


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        SEGAnalytics.track("Device Setup: Webview Screen")
        val toolbar = Ui.findView<Toolbar>(this, R.id.toolbar)
        toolbar.navigationIcon = Ui.getTintedDrawable(this, R.drawable.ic_clear_black_24dp,
                R.color.element_tint_color)

        toolbar.setNavigationOnClickListener { _ -> finish() }

        if (intent.hasExtra(EXTRA_PAGE_TITLE)) {
            toolbar.title = intent.getStringExtra(EXTRA_PAGE_TITLE)
        }

        val webView = Ui.findView<WebView>(this, R.id.web_content)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // handle redirects in the same view
                view.loadUrl(url)
                // return false to indicate that we do not want to leave the webview
                return false // then it is not handled by default action
            }
        }

        val webSettings = webView.settings
        // this has to be enabled or else some pages don't render *at all.*
        webSettings.javaScriptEnabled = true

        val uri = intent.getParcelableExtra<Uri>(EXTRA_CONTENT_URI)
        webView.loadUrl(uri.toString())
    }

    companion object {
        private const val EXTRA_CONTENT_URI = "EXTRA_CONTENT_URI"
        private const val EXTRA_PAGE_TITLE = "EXTRA_PAGE_TITLE"

        fun buildIntent(ctx: Context, uri: Uri): Intent {
            return Intent(ctx, WebViewActivity::class.java)
                    .putExtra(EXTRA_CONTENT_URI, uri)
        }

        fun buildIntent(ctx: Context, uri: Uri, pageTitle: CharSequence): Intent {
            return buildIntent(ctx, uri)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle.toString())
        }
    }

}
