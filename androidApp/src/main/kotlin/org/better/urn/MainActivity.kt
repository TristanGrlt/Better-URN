package org.better.urn

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import org.better.urn.data.AndroidContextProvider

class MainActivity : ComponentActivity() {

    private var deepLinkState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        AndroidContextProvider.init(applicationContext)

        deepLinkState = intent?.dataString

        setContent {
            App(deepLink = deepLinkState) {
                deepLinkState = null
                intent?.data = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkState = intent.dataString
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
