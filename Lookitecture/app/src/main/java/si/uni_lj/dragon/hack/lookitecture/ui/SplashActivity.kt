package si.uni_lj.dragon.hack.lookitecture.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import si.uni_lj.dragon.hack.lookitecture.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Preview
    @Composable
    private fun Main() {
        LaunchedEffect(key1 = true) {
            delay(2000)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Yellow
                ), contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(id = R.drawable.office_building), contentDescription = null)
        }
    }
}