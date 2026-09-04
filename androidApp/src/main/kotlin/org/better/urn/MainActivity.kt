package org.better.urn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import org.better.urn.data.AndroidContextProvider

class MainActivity : ComponentActivity() {

    private val deepLinkFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidContextProvider.init(applicationContext)

        intent?.dataString?.let { deepLinkFlow.value = it }

        setContent {
            val deepLink by deepLinkFlow.collectAsState()
            App(
                deepLink = deepLink,
                onDeepLinkHandled = { deepLinkFlow.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { deepLinkFlow.value = it }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
