package com.sourcepoint.cmplibrary.stub

import com.sourcepoint.cmplibrary.util.ExecutorManager

class MockExecutorManager : ExecutorManager {
    override fun executeOnMain(block: () -> Unit) {
        block()
    }
}