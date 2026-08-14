package com.og.ui.screens

import android.content.Context
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.og.Reminders
import com.og.ui.OgCard
import com.og.ui.OgField
import com.og.ui.OgViewModel
import com.og.ui.PrimaryButton
import com.og.ui.QuietButton
import com.og.ui.SectionTitle
import com.og.ui.Status
import com.og.ui.StatusBadge
import com.og.ui.UiState
import com.og.ui.theme.Og

/** Stable per-install id, so a restore lands on the right backup row. */
fun deviceIdOf(context: Context): String =
    AndroidSettings.Secure.getString(context.contentResolver, AndroidSettings.Secure.ANDROID_ID)
        ?: "og-device"

@Composable
fun SettingsScreen(state: UiState, vm: OgViewModel) {
    val context = LocalContext.current
    val status by vm.syncStatus.collectAsState()
    var url by remember(state.profile?.neonUrl) { mutableStateOf(state.profile?.neonUrl.orEmpty()) }
    val reminderOn = state.profile?.reminderOn ?: true

    LazyColumn(
        Modifier.fillMaxSize().background(Og.Canvas),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
        }

        // ---- backup ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Cloud backup")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Paste your Neon connection string to back this device up. It is stored on " +
                        "your phone only — never in the app's code, so it cannot leak through the " +
                        "public repository or the APK.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
                Spacer(Modifier.height(14.dp))
                OgField(
                    label = "Neon connection string",
                    value = url,
                    onValueChange = { url = it },
                    numeric = false,
                    placeholder = "postgresql://user:pass@host/db",
                )
                Spacer(Modifier.height(12.dp))
                QuietButton("Save connection", onClick = { vm.saveNeonUrl(url) })

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        "Back up now",
                        onClick = { vm.backupNow(deviceIdOf(context)) },
                        modifier = Modifier.weight(1f),
                    )
                    QuietButton(
                        "Restore",
                        onClick = { vm.restoreNow(deviceIdOf(context)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (status != null) {
                    Spacer(Modifier.height(14.dp))
                    val ok = status!!.startsWith("Backed up") || status!!.startsWith("Restored")
                    StatusBadge(if (ok) Status.GOOD else Status.NEUTRAL, status!!)
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Restore overwrites this device with the last backup. Everything goes up as " +
                        "one snapshot, so the newest backup always wins.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Og.InkMuted,
                )
            }
        }

        // ---- reminder ----
        item {
            OgCard(padding = 18.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Nightly reminder",
                            style = MaterialTheme.typography.titleLarge,
                            color = Og.Ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "A nudge at 11:00 pm to log the day before it rolls over.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Og.InkMuted,
                        )
                    }
                    Switch(
                        checked = reminderOn,
                        onCheckedChange = { on ->
                            vm.setReminder(on)
                            if (on) Reminders.schedule(context) else Reminders.cancel(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Og.Ink,
                            checkedBorderColor = Og.Ink,
                            uncheckedThumbColor = Og.InkMuted,
                            uncheckedTrackColor = Og.Inset,
                            uncheckedBorderColor = Og.Hairline,
                        ),
                    )
                }
            }
        }

        // ---- custom lifts ----
        item {
            OgCard(padding = 18.dp) {
                SectionTitle("Your lifts", trailing = "${state.customExercises.size}")
                Spacer(Modifier.height(6.dp))
                if (state.customExercises.isEmpty()) {
                    Text(
                        "Lifts you add from the Train tab show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                } else {
                    state.customExercises.forEach { ex ->
                        Row(
                            Modifier.fillMaxWidth().height(46.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                                Text(
                                    ex.toExercise().primary.joinToString(", ") { it.label },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Og.InkMuted,
                                )
                            }
                            Text(
                                "Remove",
                                style = MaterialTheme.typography.labelLarge,
                                color = Og.Critical,
                                modifier = Modifier.clickable { vm.deleteCustomExercise(ex.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
