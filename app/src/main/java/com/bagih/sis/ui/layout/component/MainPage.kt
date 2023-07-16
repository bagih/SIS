package com.bagih.sis.ui.layout.component

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bagih.sis.R


@Composable
fun SISTopAppBar(modifier: Modifier = Modifier, title: String,uploadButtonAction: () -> Unit,  cropButtonAction: () -> Unit) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = title)
        },
        actions = {
            IconButton(onClick = uploadButtonAction){
                Icon(painter = painterResource(id = R.drawable.cloud_upload_48px), contentDescription = "Upload Image", modifier = Modifier.height(24.dp).width(24.dp))
            }
            IconButton(onClick = cropButtonAction ) {
                Icon(painter = painterResource(id = R.drawable.ic_sharp_crop_24), contentDescription = "crop image")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SISFab(modifier: Modifier = Modifier, fabButtonOnResult: (Bitmap?) -> Unit) {
    val cameraImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = fabButtonOnResult
    )
    FloatingActionButton(
        onClick =
        { cameraImagePickerLauncher.launch() })
 {
    Icon(
        painter = painterResource(id = R.drawable.ic_baseline_camera_24),
        contentDescription = "take a picture",
        tint = Color.White
    )
}
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float
){
    Text(
        text = text,
        Modifier
            .border(1.dp, color = Color.Black)
            .weight(weight)
            .padding(8.dp)
    )
}

@Composable
fun TableRow(label: String, value: String, modifier: Modifier = Modifier){
    val col1Weight = .3f
    val col2Weight = .7f
    Row(modifier = modifier) {
        TableCell(text = label, weight = col1Weight);
        TableCell(text = value, weight = col2Weight)
    }
}

