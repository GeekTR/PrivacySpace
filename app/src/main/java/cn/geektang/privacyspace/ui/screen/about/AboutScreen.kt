package cn.geektang.privacyspace.ui.screen.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.geektang.privacyspace.BuildConfig
import cn.geektang.privacyspace.R
import cn.geektang.privacyspace.ui.widget.TopBar
import cn.geektang.privacyspace.util.LocalNavHostController
import cn.geektang.privacyspace.util.openUrl
import coil.compose.rememberImagePainter

@Preview(showSystemUi = true)
@Composable
fun AboutScreen() {
    val navController = LocalNavHostController.current
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = stringResource(R.string.about), onNavigationIconClick = {
            navController.popBackStack()
        })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val appName = stringResource(id = R.string.app_name)
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = appName
            )
            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = appName,
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "Version: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.subtitle1
            )
        }

        GroupTitle(text = "What's this")
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 10.dp),
            text = stringResource(R.string.about_page_app_intro),
            fontSize = 14.sp
        )

        GroupTitle(text = "Developers")
        val developers = remember {
            listOf(
                CardItem(
                    R.drawable.ic_avatar,
                    "GeekTR",
                    "Developer & Designer",
                    "https://github.com/GeekTR"
                ),
                CardItem(
                    R.drawable.ic_github,
                    "Source Code",
                    "https://github.com/GeekTR/PrivacySpace",
                    "https://github.com/GeekTR/PrivacySpace"
                ),
            )
        }
        developers.forEach {
            Card(cardItem = it)
        }

        val context = LocalContext.current
        val telegramAndCoolapk = remember {
            listOf(
                CardItem(
                    R.drawable.ic_telegram,
                    "Telegram",
                    context.getString(R.string.about_page_telegram_description),
                    "https://t.me/PrivacySpaceAlpha"
                ),
                CardItem(
                    R.drawable.ic_coolapk,
                    context.getString(R.string.coolapk),
                    context.getString(R.string.about_page_coolapk_description),
                    "coolmarket://u/18765870"
                ),
            )
        }
        GroupTitle(text = "Telegram & Coolapk")
        telegramAndCoolapk.forEach {
            Card(cardItem = it)
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    val isLight = MaterialTheme.colors.isLight
    val primarySurfaceColor = MaterialTheme.colors.primarySurface
    val surfaceBackgroundColor = remember(isLight) {
        if (isLight) {
            Color(0xfff7f7f7)
        } else {
            primarySurfaceColor
        }
    }
    Surface(modifier = Modifier.fillMaxWidth(), color = surfaceBackgroundColor, elevation = 2.dp) {
        Text(
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 10.dp),
            text = text
        )
    }
}

@Composable
private fun Card(cardItem: CardItem) {
    val isLight = MaterialTheme.colors.isLight
    val textColor = remember(isLight) {
        if (isLight) {
            Color(0xde000000)
        } else {
            Color(0xdeffffff)
        }
    }
    val hintColor = remember(isLight) {
        if (isLight) {
            Color(0xff757575)
        } else {
            Color(0x80ffffff)
        }
    }

    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clickable {
                try {
                    context.openUrl(cardItem.homePage)
                } catch (e: Exception) {
                }
            }
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .size(38.dp),
            painter = rememberImagePainter(data = cardItem.avatar),
            contentDescription = "avatar",
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(text = cardItem.name, fontSize = 14.sp, color = textColor)
            Text(text = cardItem.description, fontSize = 12.sp, color = hintColor)
        }
    }
}

class CardItem(
    @DrawableRes val avatar: Int,
    val name: String,
    val description: String,
    val homePage: String
)