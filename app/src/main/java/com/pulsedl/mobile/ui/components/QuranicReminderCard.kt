package com.pulsedl.mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedl.mobile.ui.theme.CyanStream
import com.pulsedl.mobile.ui.theme.EmeraldSuccess
import com.pulsedl.mobile.ui.theme.ObsidianBorder
import com.pulsedl.mobile.ui.theme.ObsidianCanvas
import com.pulsedl.mobile.ui.theme.TextPrimary
import com.pulsedl.mobile.ui.theme.TextSecondary
import com.pulsedl.mobile.ui.theme.obsidianGlassCard
import kotlinx.coroutines.delay

data class QuranicReminderItem(
    val arabicText: String,
    val reference: String,
    val reflectionEnglish: String
)

private val CURATED_QURANIC_REMINDERS = listOf(
    QuranicReminderItem(
        arabicText = "﴿إِنَّ السَّمْعَ وَالْبَصَرَ وَالْفُؤَادَ كُلُّ أُولَٰئِكَ كَانَ عَنْهُ مَسْئُولًا﴾",
        reference = "سورة الإسراء : 36 • Surah Al-Isra [17:36]",
        reflectionEnglish = "Indeed, the hearing, the sight, and the heart — about all those one will be questioned. Use media for beneficial knowledge and pure content."
    ),
    QuranicReminderItem(
        arabicText = "﴿قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ وَيَحْفَظُوا فُرُوجَهُمْ ۚ ذَٰلِكَ أَزْكَىٰ لَهُمْ﴾",
        reference = "سورة النور : 30 • Surah An-Nur [24:30]",
        reflectionEnglish = "Tell the believing men to lower their gaze and guard their modesty; that is purer for them."
    ),
    QuranicReminderItem(
        arabicText = "﴿وَالَّذِينَ هُمْ عَنِ اللَّغْوِ مُعْرِضُونَ﴾",
        reference = "سورة المؤمنون : 3 • Surah Al-Mu'minun [23:3]",
        reflectionEnglish = "Successful indeed are the believers — those who turn away from vain and ill speech."
    ),
    QuranicReminderItem(
        arabicText = "﴿وَقُل رَّبِّ زِدْنِي عِلْمًا﴾",
        reference = "سورة طه : 114 • Surah Taha [20:114]",
        reflectionEnglish = "And say: 'My Lord, increase me in beneficial knowledge.'"
    ),
    QuranicReminderItem(
        arabicText = "«مِنْ حُسْنِ إِسْلَامِ الْمَرْءِ تَرْكُهُ مَا لَا يَعْنِيهِ»",
        reference = "حديث نبوي شريف • رواه الترمذي",
        reflectionEnglish = "Part of the perfection of a person's Islam is leaving that which does not concern or benefit him."
    )
)

/**
 * Contextual Quranic Verses & Virtue Banner (`QuranicReminderCard.kt`).
 * Rendered in the top slot where commercial downloaders place intrusive banner ads.
 */
@Composable
fun QuranicReminderCard(
    modifier: Modifier = Modifier
) {
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(14_000L)
            index = (index + 1) % CURATED_QURANIC_REMINDERS.size
        }
    }

    val currentItem = CURATED_QURANIC_REMINDERS[index]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .obsidianGlassCard(
                cornerRadius = 18.dp,
                borderColor = EmeraldSuccess.copy(alpha = 0.35f)
            )
            .clickable {
                index = (index + 1) % CURATED_QURANIC_REMINDERS.size
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ETHICAL MINDFULNESS REMINDER • تذكير إيماني",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldSuccess
                )
            }

            IconButton(
                onClick = { index = (index + 1) % CURATED_QURANIC_REMINDERS.size },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Next Verse",
                    tint = CyanStream,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = currentItem,
            transitionSpec = {
                fadeIn(tween(260)) togetherWith fadeOut(tween(200))
            },
            label = "QuranicVerseTransition"
        ) { verse ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCanvas.copy(alpha = 0.55f))
                    .border(1.dp, ObsidianBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = verse.arabicText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.End,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = verse.reflectionEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = verse.reference,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanStream
                )
            }
        }
    }
}
