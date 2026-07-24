package com.iconchanger.wallpaper.rolling.icons

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.ads.banner_ads.CSCBanner
import com.cscmobi.libraryads.commons.sharepreference.CSCSPF
import com.cscmobi.libraryads.data.AdsLanguageConfig
import com.cscmobi.libraryads.data.AdsOBConfig
import com.cscmobi.libraryads.data.AdsSplashConfig
import com.cscmobi.libraryads.data.LanguageConfig
import com.cscmobi.libraryads.data.LanguageSetting
import com.cscmobi.libraryads.data.OBConfig
import com.cscmobi.libraryads.data.OnActivityCallBack
import com.cscmobi.libraryads.data.SplashConfig
import com.cscmobi.libraryads.data.UiLanguageConfig
import com.cscmobi.libraryads.data.UiOBConfig
import com.cscmobi.libraryads.data.UiSplashConfig
import com.iconchanger.wallpaper.rolling.icons.ui.MainActivity
import com.iconchanger.wallpaper.rolling.icons.ui.PermissionActivity
import com.iconchanger.wallpaper.rolling.icons.utils.EnumSelectLanguage
import com.iconchanger.wallpaper.rolling.icons.utils.RemoteConfigs
import com.iconchanger.wallpaper.rolling.icons.utils.SharePreferenceUtils
import com.iconchanger.wallpaper.rolling.icons.utils.SystemUtil


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initFO()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val savedLangCode = CSCSPF(this).language_code_selected
        val localeCode = if (savedLangCode.isNullOrBlank()) {
            "en"
        } else {
            savedLangCode
        }
        SystemUtil.saveLocale(this, localeCode)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty || currentLocales.get(0)?.language != localeCode) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeCode))
        }

//        Executors.newSingleThreadExecutor().execute {
//            FirebaseApp.initializeApp(this)
//            FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
//            AdjustHelper.init(
//                application = this,
//                appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
//                iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
//                isDebug = BuildConfig.DEBUG
//            )
//        }

    }

    private fun initFO() {
        val cscLibrary = CSCApplication(this, RemoteConfigs, BuildConfig.DEBUG)
        val isOrganic = SharePreferenceUtils.isOrganic(this)
        val nativeFullId = if (isOrganic) "" else getString(R.string.native_splash_full)
        val nativeFullHighId = if (isOrganic) "" else getString(R.string.native_splash_full_high)
        val nativeOB2Id = if (isOrganic) "" else getString(R.string.native_onboarding_2)
        val nativeOB3Id = if (isOrganic) "" else getString(R.string.native_onboarding_3)
        val nativeOBFull1Id = if (isOrganic) "" else getString(R.string.native_onboarding_full_1)
        val nativeOBFull2Id = if (isOrganic) "" else getString(R.string.native_onboarding_full_2)

        cscLibrary.initSdk(
            adjustAppToken = "",
            gsmAppId = "6a60762b359ab2ad822596f2",
            splashConfig = SplashConfig(
                uiSplashConfig = UiSplashConfig(
                    resLayout = R.layout.activity_splash,
                    showFOForever = false,
                    homeActivity = PermissionActivity::class.java,
                    timeout = 30_000
                ),
                adsSplashConfig = AdsSplashConfig(
                    bannerId = getString(R.string.banner_splash),
                    interHighId = getString(R.string.inter_splash_high),
                    interAllId = getString(R.string.inter_splash),
                    nativeFullId = getString(R.string.native_splash_full),
                    nativeFullHighId = getString(R.string.native_splash_full_high),
                    nativeFullLayout = R.layout.layout_native_full,
                    admobAOAId = getString(R.string.resume_open_app),
                )
            ),
            languageConfig =
                LanguageConfig(
                    uiLanguageConfig = UiLanguageConfig(
                        resLayout = R.layout.activity_language_app,
                        itemLangDefault = R.layout.item_select_language_default,
                        itemLangSelected = R.layout.item_select_language_selected,
                        listLanguage = EnumSelectLanguage.toLanguageModelList(),
                        languageSetting = object : LanguageSetting {
                            override fun onDone(activity: Activity) {
                                val code = CSCSPF(activity).language_code_selected
                                SystemUtil.saveLocale(activity, code)
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(code)
                                )
                                val intent = Intent(activity, PermissionActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    putExtra("disable_animation", true)
                                }
                                activity.startActivity(intent)
                            }
                        }
                    ),
                    adsLanguageConfig = AdsLanguageConfig(
                        nativeLangHighId = getString(R.string.native_language_high),
                        nativeLangId = getString(R.string.native_language),
                        nativeLangClickHighId = getString(R.string.native_language_high_click),
                        nativeLangClickId = getString(R.string.native_language_click),
                        layoutNative = R.layout.layout_native_media,
                        layoutNativeClick = R.layout.layout_native_media
                    )
                ),
            obConfig =
                OBConfig(
                    uiOBConfig = UiOBConfig(
                        resFragmentOB1 = R.layout.fragment_intro1,
                        resFragmentOB2 = R.layout.fragment_intro2,
                        resFragmentOB3 = R.layout.fragment_intro3,
                        resFragmentOB4 = R.layout.fragment_intro4,
                        resFragmentOBAdFull = R.layout.fragment_ob_ad_full,
                        activityCallback = object : OnActivityCallBack {
                            override fun onNextActivity(activity: Activity, inSession2: Boolean) {
                                if (inSession2) {
                                    val intent = Intent(activity, PermissionActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    activity.startActivity(intent)
                                }
                            }
                        }
                    ),
                    adsOBConfig = AdsOBConfig(
                        nativeOB1Id = getString(R.string.native_onboarding_1),
                        nativeOB4Id = getString(R.string.native_onboarding_4),
                        nativeOBFull1Id = nativeOBFull1Id,
                        nativeOBFull2Id = nativeOBFull2Id,
                        layoutNativeOB1 = R.layout.layout_native_media,
                        layoutNativeOB4 = R.layout.layout_native_media,
                        layoutNativeFullOB = R.layout.admob_layout_native_full,
                        isLoadNativeOBInLanguage = true
                    )
                ))
    }
}
