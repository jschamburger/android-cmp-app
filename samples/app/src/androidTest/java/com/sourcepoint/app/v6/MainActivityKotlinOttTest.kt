package com.sourcepoint.app.v6

import android.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.uitestutil.*
import com.sourcepoint.app.v6.TestUseCase.Companion.clickOnGdprReviewConsent
import com.sourcepoint.app.v6.TestUseCase.Companion.tapAcceptAllOnWebView
import com.sourcepoint.app.v6.core.DataProvider
import com.sourcepoint.app.v6.di.customCategoriesDataProd
import com.sourcepoint.app.v6.di.customVendorDataListProd
import com.sourcepoint.cmplibrary.SpClient
import com.sourcepoint.cmplibrary.creation.config
import com.sourcepoint.cmplibrary.exception.CampaignType
import com.sourcepoint.cmplibrary.model.MessageLanguage
import com.sourcepoint.cmplibrary.model.exposed.SpConfig
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module

@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityKotlinOttTest {

    lateinit var scenario: ActivityScenario<MainActivityKotlin>

    private val device by lazy { UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) }

    @After
    fun cleanup() {
        if (this::scenario.isLateinit) scenario.close()
    }

    private val spConfOtt = config {
        accountId = 22
        propertyName = "ott.test.suite"
        messLanguage = MessageLanguage.ENGLISH
        messageTimeout = 3000
        +(CampaignType.GDPR)
    }

    @Test
    fun GIVEN_an_OTT_campaign_SHOW_message_and_ACCEPT_ALL() = runBlocking<Unit> {

        val spClient = mockk<SpClient>(relaxed = true)

        loadKoinModules(
            mockModule(
                spConfig = spConfOtt,
                gdprPmId = "579231",
                ccpaPmId = "1",
                isOtt = true,
                spClientObserver = listOf(spClient)
            )
        )

        scenario = launchActivity()

        periodicWr(backup = { scenario.recreateAndResume() }) {
            tapAcceptAllOnWebView()
            device.pressEnter()
        }

        verify(exactly = 0) { spClient.onError(any()) }
        wr{ verify(exactly = 1) { spClient.onConsentReady(any()) } }
        verify { spClient.onAction(any(), withArg { it.pubData["pb_key"].assertEquals("pb_value") }) }

        wr {
            verify {
                spClient.run {
                    onUIReady(any())
                    onUIFinished(any())
                    onAction(any(), any())
                    onConsentReady(any())
                }
            }
        }


        scenario.onActivity { activity ->
            val IABTCF_TCString = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("IABTCF_TCString", null)
            IABTCF_TCString.assertNotNull()
        }

    }

    @Test
    fun GIVEN_an_OTT_campaign_SHOW_message_and_ACCEPT_ALL_from_PM() = runBlocking<Unit> {

        val spClient = mockk<SpClient>(relaxed = true)

        loadKoinModules(
            mockModule(
                spConfig = spConfOtt,
                gdprPmId = "579231",
                ccpaPmId = "1",
                isOtt = true,
                spClientObserver = listOf(spClient)
            )
        )

        scenario = launchActivity()

        periodicWr(backup = { scenario.recreateAndResume() }) {
            tapAcceptAllOnWebView()
            device.pressEnter()
        }

        wr { clickOnGdprReviewConsent() }
        wr(backup = { clickOnGdprReviewConsent() }){
            tapAcceptAllOnWebView()
            device.pressEnter()
        }

        verify(exactly = 0) { spClient.onError(any()) }
        wr{ verify(exactly = 2) { spClient.onConsentReady(any()) } }
        verify { spClient.onAction(any(), withArg { it.pubData["pb_key"].assertEquals("pb_value") }) }

        wr {
            verify {
                spClient.run {
                    onUIReady(any())
                    onUIFinished(any())
                    onAction(any(), any())
                    onConsentReady(any())
                }
            }
        }


        scenario.onActivity { activity ->
            val IABTCF_TCString = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("IABTCF_TCString", null)
            IABTCF_TCString.assertNotNull()
        }

    }

    private fun mockModule(
        spConfig: SpConfig,
        gdprPmId: String,
        ccpaPmId: String = "",
        uuid: String? = null,
        url: String = "",
        isOtt: Boolean = false,
        spClientObserver: List<SpClient> = emptyList()
    ): Module {
        return module(override = true) {
            single<List<SpClient?>> { spClientObserver }
            single<DataProvider> {
                object : DataProvider {
                    override val authId = uuid
                    override val resetAll = true
                    override val isOtt = isOtt
                    override val url = url
                    override val spConfig: SpConfig = spConfig
                    override val gdprPmId: String = gdprPmId
                    override val ccpaPmId: String = ccpaPmId
                    override val customVendorList: List<String> = customVendorDataListProd.map { it.first }
                    override val customCategories: List<String> = customCategoriesDataProd.map { it.first }
                }
            }
        }
    }

}