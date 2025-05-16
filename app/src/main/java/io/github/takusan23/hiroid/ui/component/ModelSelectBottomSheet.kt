package io.github.takusan23.hiroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.hiroid.R
import io.github.takusan23.hiroid.tool.VoskModelTool
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectBottomSheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val modelList = remember { mutableStateOf<List<File>?>(null) }

    // 読み込み
    LaunchedEffect(key1 = Unit) {
        modelList.value = VoskModelTool.getVoskModelList(context)
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onClose
    ) {
        LazyColumn {

            item {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = "モデルの選択",
                    fontSize = 24.sp
                )
            }

            if (modelList.value == null) {
                // 読み込み中
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                itemsIndexed(modelList.value!!) { index, model ->

                    if (index != 0) {
                        HorizontalDivider()
                    }

                    Surface(
                        onClick = { },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = model.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 18.sp
                            )
                            Icon(
                                painter = painterResource(R.drawable.check_circle_24px),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}