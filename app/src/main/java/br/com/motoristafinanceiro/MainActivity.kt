package br.com.motoristafinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.motoristafinanceiro.ui.navigation.AppNav
import br.com.motoristafinanceiro.ui.theme.MotoristaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotoristaTheme {
                AppNav()
            }
        }
    }
}
