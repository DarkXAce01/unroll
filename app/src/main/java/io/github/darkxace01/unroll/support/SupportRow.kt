package io.github.darkxace01.unroll.support

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darkxace01.unroll.app

/**
 * The quiet line after an export. Two small text buttons, never a pop-up, never in the way. Hidden
 * entirely in builds without tips or ads.
 */
@Composable
fun SupportLine(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.app
    if (!app.ads.supported && !app.tips.supported) return
    var said by remember { mutableStateOf<String?>(null) }
    var tipsOpen by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            said ?: "Unroll is free and always will be. If it helped, you can support it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (app.ads.supported) TextButton(onClick = {
                (context as? Activity)?.let { a ->
                    said = "Loading an ad…"
                    app.ads.show(a) { outcome ->
                        said = when (outcome) {
                            AdOutcome.Thanked -> { app.prefs.addSupport(); "Thank you. That really helps." }
                            AdOutcome.Closed -> null
                            AdOutcome.Unavailable -> "No ad available right now. Thanks for offering."
                        }
                    }
                }
            }) { Text("Watch an ad") }
            if (app.tips.supported) TextButton(onClick = { tipsOpen = !tipsOpen }) { Text("Leave a tip") }
        }
        if (tipsOpen) TipButtons()
    }
}

@Composable
fun TipButtons() {
    val context = LocalContext.current
    val app = context.app
    LaunchedEffect(Unit) { app.tips.refresh() }
    val state by app.tips.state.collectAsStateWithLifecycle()
    var thanks by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        app.tips.outcomes.collect {
            thanks = when (it) {
                is TipOutcome.Thanked -> "Thank you so much."
                TipOutcome.Pending -> "Thanks! It'll go through when the payment clears."
                TipOutcome.Failed -> "That didn't go through. Nothing was charged."
            }
        }
    }
    when (val s = state) {
        TipJarState.Loading -> Text("Loading…", style = MaterialTheme.typography.bodySmall)
        TipJarState.Unavailable -> Text("Tips need the Play Store.", style = MaterialTheme.typography.bodySmall)
        is TipJarState.Ready -> Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tip.entries.forEach { tip ->
                val price = s.prices[tip] ?: return@forEach
                val name = when (tip) { Tip.COFFEE -> "Coffee"; Tip.LUNCH -> "Lunch"; Tip.DINNER -> "Dinner" }
                OutlinedButton(onClick = { (context as? Activity)?.let { app.tips.buy(it, tip) } }) { Text("$name $price") }
            }
        }
    }
    thanks?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
