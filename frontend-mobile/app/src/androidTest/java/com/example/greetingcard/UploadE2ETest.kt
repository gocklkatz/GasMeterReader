package com.example.greetingcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.greetingcard.ui.camera.CameraViewModel
import com.example.greetingcard.ui.camera.UploadState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class UploadE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun photoUploadedByMobileAppearsInReadings() {
        // 1. Drive the login UI (LoginScreen is the start destination when no token is stored)
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("admin")
        composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput("changeme")
        composeTestRule.onNodeWithText("Login").performClick()

        // 2. Wait for CameraScreen — "Logout" button is always visible there
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Logout").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Create a valid JPEG programmatically (bypasses CameraX — Option A)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile = createTestJpeg(context)

        // 4. Obtain the CameraViewModel that CameraScreen is using and trigger the upload
        val viewModel = ViewModelProvider(composeTestRule.activity)[CameraViewModel::class.java]
        composeTestRule.runOnUiThread { viewModel.onPhotoCaptured(testFile) }

        // 5. Wait up to 60 s for WorkManager to upload the image to the backend.
        //    The ViewModel transitions to UploadState.Success only when the worker reports
        //    WorkInfo.State.SUCCEEDED, which requires a real HTTP 201 from the backend.
        //    60 s gives room for one failed attempt + the 30 s exponential backoff retry.
        composeTestRule.waitUntil(timeoutMillis = 60_000) {
            viewModel.uiState.value.uploadState is UploadState.Success
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Creates a 200×200 grey JPEG in the app's cache directory. */
    private fun createTestJpeg(context: Context): File {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.DKGRAY)
        val file = File(context.cacheDir, "test_meter_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return file
    }
}
