package com.sourcepointmeta.metaapp

import com.example.uitestutil.* // ktlint-disable

class TestUseCaseMeta {
    companion object {

        fun checkMessageDisplayed() {
            isDisplayedAllOfByResId(resId = R.id.message)
        }

        fun tapFab() {
            performClickById(R.id.fab)
        }

        fun addTestProperty() {
            addProperty(
                propertyName = "mobile.demo",
                accountId = "22",
                gdprPmId = "12",
                ccpaPmId = "3",
                autId = "auth",
                gdprTps = listOf(Pair("a", "a"), Pair("b", "b"), Pair("c", "c")),
                ccpaTps = listOf(Pair("d", "d"), Pair("e", "e"))
            )
        }

        fun saveProperty() = scrollAndPerformClickById(R.id.save_btn)

        fun addProperty(
            propertyName: String,
            accountId: String,
            gdprPmId: String,
            ccpaPmId: String? = null,
            autId: String? = null,
            gdprTps: List<Pair<String, String>>? = null,
            ccpaTps: List<Pair<String, String>>? = null
        ) {
            addTextById(R.id.prop_name_ed, propertyName)
            addTextById(R.id.account_id_ed, accountId)
            addTextById(R.id.gdpr_pm_id_ed, gdprPmId)
            ccpaPmId?.let { addTextById(R.id.ccpa_pm_id_ed, it) }
            autId?.let { addTextById(R.id.auth_id_ed, it) }
            gdprTps?.let {
                it.forEach { tp ->
                    scrollAndPerformClickById(R.id.btn_targeting_params_gdpr)
                    addTextById(R.id.tp_key_ed, tp.first)
                    addTextById(R.id.tp_value_et, tp.second)
                    pressAlertDialogBtn("CREATE")
                }
            }
            ccpaTps?.let {
                it.forEach { tp ->
                    scrollAndPerformClickById(R.id.btn_targeting_params_ccpa)
                    addTextById(R.id.tp_key_ed, tp.first)
                    addTextById(R.id.tp_value_et, tp.second)
                    pressAlertDialogBtn("CREATE")
                }
            }
        }
    }
}