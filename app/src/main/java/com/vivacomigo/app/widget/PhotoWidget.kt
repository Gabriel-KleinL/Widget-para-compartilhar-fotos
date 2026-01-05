package com.vivacomigo.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vivacomigo.app.R
import androidx.glance.appwidget.action.actionStartActivity
import com.vivacomigo.app.MainActivity

class PhotoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            PhotoWidgetContent(context)
        }
    }

    @Composable
    private fun PhotoWidgetContent(context: Context) {
        val photoUrl = PhotoWidgetDataStore.getPhotoUrl(context)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.primary))
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null && photoUrl.isNotEmpty()) {
                // Show photo - in a real implementation, we'd load from URL
                // For now, show a placeholder
                Text(
                    text = "❤️",
                    style = TextStyle(
                        fontSize = 48.sp,
                        color = ColorProvider(R.color.white)
                    )
                )
            } else {
                // Show empty state
                Text(
                    text = context.getString(R.string.tap_to_add),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(R.color.white)
                    ),
                    modifier = GlanceModifier.padding(16.dp)
                )
            }
        }
    }
}
