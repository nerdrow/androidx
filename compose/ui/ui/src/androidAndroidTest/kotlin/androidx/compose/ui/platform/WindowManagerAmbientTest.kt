/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.platform

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowManagerAmbientTest {
    @get:Rule
    val rule = createComposeRule()

    @Ignore("Flaky Test b/173088588")
    @Test
    fun windowIsFocused_onLaunch() {
        // Arrange.
        lateinit var windowManager: WindowManager
        val windowFocusGain = CountDownLatch(1)
        rule.setContent {
            BasicText("Main Window")
            windowManager = AmbientWindowManager.current
            WindowFocusObserver { if (it) windowFocusGain.countDown() }
        }

        // Act.
        rule.waitForIdle()

        // Assert.
        windowFocusGain.await(5, SECONDS)
        assertThat(windowManager.isWindowFocused).isTrue()
    }

    @Test
    fun mainWindowIsNotFocused_whenPopupIsVisible() {
        // Arrange.
        lateinit var mainWindowManager: WindowManager
        lateinit var popupWindowManager: WindowManager
        val mainWindowFocusLoss = CountDownLatch(1)
        val popupFocusGain = CountDownLatch(1)
        val showPopup = mutableStateOf(false)
        rule.setContent {
            BasicText("Main Window")
            mainWindowManager = AmbientWindowManager.current
            WindowFocusObserver { if (!it) mainWindowFocusLoss.countDown() }
            if (showPopup.value) {
                Popup(isFocusable = true, onDismissRequest = { showPopup.value = false }) {
                    BasicText("Popup Window")
                    popupWindowManager = AmbientWindowManager.current
                    WindowFocusObserver { if (it) popupFocusGain.countDown() }
                }
            }
        }

        // Act.
        rule.runOnIdle { showPopup.value = true }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusLoss.await(5, SECONDS)).isTrue()
        assertThat(popupFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowManager.isWindowFocused).isFalse()
        assertThat(popupWindowManager.isWindowFocused).isTrue()
    }

    @Test
    fun windowIsFocused_whenPopupIsDismissed() {
        // Arrange.
        lateinit var mainWindowManager: WindowManager
        var mainWindowFocusGain = CountDownLatch(1)
        val popupFocusGain = CountDownLatch(1)
        val showPopup = mutableStateOf(false)
        rule.setContent {
            BasicText(text = "Main Window")
            mainWindowManager = AmbientWindowManager.current
            WindowFocusObserver { if (it) mainWindowFocusGain.countDown() }
            if (showPopup.value) {
                Popup(isFocusable = true, onDismissRequest = { showPopup.value = false }) {
                    BasicText(text = "Popup Window")
                    WindowFocusObserver { if (it) popupFocusGain.countDown() }
                }
            }
        }
        rule.runOnIdle { showPopup.value = true }
        rule.waitForIdle()
        assertThat(popupFocusGain.await(5, SECONDS)).isTrue()
        mainWindowFocusGain = CountDownLatch(1)

        // Act.
        rule.runOnIdle { showPopup.value = false }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowManager.isWindowFocused).isTrue()
    }

    @Test
    fun mainWindowIsNotFocused_whenDialogIsVisible() {
        // Arrange.
        lateinit var mainWindowManager: WindowManager
        lateinit var dialogWindowManager: WindowManager
        val mainWindowFocusLoss = CountDownLatch(1)
        val dialogFocusGain = CountDownLatch(1)
        val showDialog = mutableStateOf(false)
        rule.setContent {
            BasicText("Main Window")
            mainWindowManager = AmbientWindowManager.current
            WindowFocusObserver { if (!it) mainWindowFocusLoss.countDown() }
            if (showDialog.value) {
                Dialog(onDismissRequest = { showDialog.value = false }) {
                    BasicText("Popup Window")
                    dialogWindowManager = AmbientWindowManager.current
                    WindowFocusObserver { if (it) dialogFocusGain.countDown() }
                }
            }
        }

        // Act.
        rule.runOnIdle { showDialog.value = true }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusLoss.await(5, SECONDS)).isTrue()
        assertThat(dialogFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowManager.isWindowFocused).isFalse()
        assertThat(dialogWindowManager.isWindowFocused).isTrue()
    }

    @Test
    fun windowIsFocused_whenDialogIsDismissed() {
        // Arrange.
        lateinit var mainWindowManager: WindowManager
        var mainWindowFocusGain = CountDownLatch(1)
        val dialogFocusGain = CountDownLatch(1)
        val showDialog = mutableStateOf(false)
        rule.setContent {
            BasicText(text = "Main Window")
            mainWindowManager = AmbientWindowManager.current
            WindowFocusObserver { if (it) mainWindowFocusGain.countDown() }
            if (showDialog.value) {
                Dialog(onDismissRequest = { showDialog.value = false }) {
                    BasicText(text = "Popup Window")
                    WindowFocusObserver { if (it) dialogFocusGain.countDown() }
                }
            }
        }
        rule.runOnIdle { showDialog.value = true }
        rule.waitForIdle()
        assertThat(dialogFocusGain.await(5, SECONDS)).isTrue()
        mainWindowFocusGain = CountDownLatch(1)

        // Act.
        rule.runOnIdle { showDialog.value = false }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowManager.isWindowFocused).isTrue()
    }
}
