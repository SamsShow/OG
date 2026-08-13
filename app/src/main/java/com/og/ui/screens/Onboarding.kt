package com.og.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.og.ui.OgCard
import com.og.ui.OgField
import com.og.ui.PrimaryButton
import com.og.ui.SectionTitle
import com.og.ui.theme.Og

@Composable
fun OnboardingScreen(
    onDone: (
        heightCm: Double, weightKg: Double, bodyFatPct: Double, smmKg: Double,
        shouldersCm: Double?, waistCm: Double?, chestCm: Double?, armCm: Double?,
        onCreatine: Boolean,
    ) -> Unit,
) {
    var height by remember { mutableStateOf("179") }
    var weight by remember { mutableStateOf("69") }
    var bodyFat by remember { mutableStateOf("19") }
    var smm by remember { mutableStateOf("32.5") }
    var shoulders by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }
    var creatine by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Og.Canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("OG", style = MaterialTheme.typography.displayMedium, color = Og.Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your baseline. Every projection is measured against these numbers, so get them close — you can correct them later.",
            style = MaterialTheme.typography.bodyLarge,
            color = Og.InkSecondary,
        )

        Spacer(Modifier.height(28.dp))
        SectionTitle("Body composition")
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OgField("Height", height, { height = it }, Modifier.weight(1f), "cm")
            OgField("Weight", weight, { weight = it }, Modifier.weight(1f), "kg")
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OgField("Body fat", bodyFat, { bodyFat = it }, Modifier.weight(1f), "%")
            OgField("Muscle", smm, { smm = it }, Modifier.weight(1f), "kg")
        }

        Spacer(Modifier.height(28.dp))
        SectionTitle("Tape measurements", trailing = "optional")
        Spacer(Modifier.height(8.dp))
        Text(
            "Shoulders and waist matter most — their ratio is what a V-taper actually is. Without them the app tracks your weight but not your shape.",
            style = MaterialTheme.typography.bodyMedium,
            color = Og.InkMuted,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OgField("Shoulders", shoulders, { shoulders = it }, Modifier.weight(1f), "cm")
            OgField("Waist", waist, { waist = it }, Modifier.weight(1f), "cm")
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OgField("Chest", chest, { chest = it }, Modifier.weight(1f), "cm")
            OgField("Arm", arm, { arm = it }, Modifier.weight(1f), "cm")
        }

        Spacer(Modifier.height(24.dp))
        OgCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Starting creatine today", style = MaterialTheme.typography.titleMedium, color = Og.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Creatine adds 1–2 kg of water in the first few weeks. Flagging the date lets the app ignore that jump instead of reading it as fat gain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Og.InkMuted,
                    )
                }
                Spacer(Modifier.height(1.dp))
                Switch(
                    checked = creatine,
                    onCheckedChange = { creatine = it },
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

        Spacer(Modifier.height(28.dp))
        PrimaryButton("Start tracking", onClick = {
            onDone(
                height.toDoubleOrNull() ?: 179.0,
                weight.toDoubleOrNull() ?: 69.0,
                bodyFat.toDoubleOrNull() ?: 19.0,
                smm.toDoubleOrNull() ?: 32.5,
                shoulders.toDoubleOrNull(),
                waist.toDoubleOrNull(),
                chest.toDoubleOrNull(),
                arm.toDoubleOrNull(),
                creatine,
            )
        })
        Spacer(Modifier.height(40.dp))
    }
}
