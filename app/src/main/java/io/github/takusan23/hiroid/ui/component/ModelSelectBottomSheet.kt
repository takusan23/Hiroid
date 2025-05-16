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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import io.github.takusan23.hiroid.R
import io.github.takusan23.hiroid.tool.DataStoreKey
import io.github.takusan23.hiroid.tool.VoskModelTool
import io.github.takusan23.hiroid.tool.dataStore
import kotlinx.coroutines.launch
import java.io.File

/** モデル選択ボトムシート */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectBottomSheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // モデル一覧と、選択中モデル
    val modelList = remember { mutableStateOf<List<File>?>(null) }
    val currentModelFilePath = remember { mutableStateOf<String?>(null) }

    // 読み込み
    LaunchedEffect(key1 = Unit) {
        modelList.value = VoskModelTool.getVoskModelList(context)
    }

    // 選択中モデル
    LaunchedEffect(key1 = Unit) {
        context.dataStore.data.collect {
            currentModelFilePath.value = it[DataStoreKey.MODEL_FILE_PATH]
        }
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
                // 一覧表示
                itemsIndexed(modelList.value!!) { index, model ->
                    if (index != 0) {
                        HorizontalDivider()
                    }
                    ModelListItem(
                        file = model,
                        isCheck = currentModelFilePath.value == model.path,
                        onClick = {
                            // 更新
                            scope.launch {
                                context.dataStore.edit {
                                    it[DataStoreKey.MODEL_FILE_PATH] = model.path
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelListItem(
    modifier: Modifier = Modifier,
    file: File,
    isCheck: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = file.name,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp
            )
            if (isCheck) {
                Icon(
                    painter = painterResource(R.drawable.check_circle_24px),
                    contentDescription = null
                )
            }
        }
    }
}