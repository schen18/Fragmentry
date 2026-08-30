package dissonance.cunninglinguist.fragmentry.core.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val DarkColorScheme = darkColorScheme(
    primary = MoonWhite,
    secondary = GhostlyGray,
    tertiary = AmberPatina,
    background = VoidBlack,
    surface = DeepCharcoal,
    onPrimary = VoidBlack,
    onSecondary = MoonWhite,
    onTertiary = VoidBlack,
    onBackground = MoonWhite,
    onSurface = MoonWhite
)

private val LightColorScheme = lightColorScheme(
    primary = InkBlack,
    secondary = CharcoalGray,
    tertiary = AmberPatina,
    background = MistWhite,
    surface = SoftGray,
    onPrimary = MistWhite,
    onSecondary = InkBlack,
    onTertiary = InkBlack,
    onBackground = InkBlack,
    onSurface = InkBlack
)

@Composable
fun FragmentryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                Atmosphere.VoidGrain()
            }
        }
    )
}
