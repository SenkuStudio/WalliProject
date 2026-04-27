package com.walli.wallpaper.ui.screens.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walli.wallpaper.R

@Composable
fun AboutRoute(
    onBack: () -> Unit,
) {
    AboutScreen(onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Walli Wallpaper",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            AboutItem(
                icon = Icons.Rounded.Star,
                title = "Rate App",
                subtitle = "Support us on Play Store",
                onClick = { openPlayStore(context) }
            )
            AboutItem(
                icon = Icons.Rounded.Share,
                title = "Share App",
                subtitle = "Share with friends and family",
                onClick = { shareApp(context) }
            )
            AboutItem(
                icon = Icons.Rounded.Email,
                title = "Contact Us",
                subtitle = "Send us your feedback or queries",
                onClick = { contactUs(context) }
            )
            AboutItem(
                icon = Icons.Rounded.Apps,
                title = "More Apps",
                subtitle = "Check out our other applications",
                onClick = { openMoreApps(context) }
            )
            AboutItem(
                icon = Icons.Rounded.Security,
                title = "Privacy Policy",
                subtitle = "Read our privacy policy",
                onClick = { openPrivacyPolicy(context) }
            )
        }
    }
}

@Composable
fun AboutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun openPlayStore(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

private fun shareApp(context: Context) {
    val packageName = context.packageName
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out this amazing wallpaper app: https://play.google.com/store/apps/details?id=$packageName")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

private fun contactUs(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@walli.com")
        putExtra(Intent.EXTRA_SUBJECT, "Feedback for Walli App")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle if no email app is installed
    }
}

private fun openMoreApps(context: Context) {
    // Replace with your developer name/id on Play Store
    val devName = "Walli+Wallpaper"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:$devName")))
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=pub:$devName")))
    }
}

private fun openPrivacyPolicy(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.walli.com/privacy-policy"))
    context.startActivity(intent)
}
