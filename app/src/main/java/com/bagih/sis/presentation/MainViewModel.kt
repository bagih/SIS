package com.bagih.sis.presentation

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.bagih.sis.R
import com.bagih.sis.data.Cattle
import com.bagih.sis.data.cattleGender
import com.bagih.sis.utils.getFile
import com.bagih.sis.utils.loadPhotosFromExternalStorage
import com.bagih.sis.utils.predict
import com.bagih.sis.utils.uriToBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.support.label.Category
import java.io.File
import com.bagih.sis.utils.saveBitmap as saveBitmapUtils
import com.bagih.sis.utils.uploadImage as uploadImageUtils

class MainViewModel : ViewModel() {
    private var _imagePicked =
        MutableStateFlow<Uri>(Uri.parse("android.resource://com.bagih.sis/" + R.drawable.placeholder))
    var imagePicked = _imagePicked.asStateFlow()

    private var _predictionResults = MutableStateFlow<List<Category>>(listOf())
    var predictionResults = _predictionResults.asStateFlow()

    fun getCattleBio(predictionResults: List<Category>): Cattle? {
        if (predictionResults.isNotEmpty()) {
            return Cattle(
                name = predictionResults.get(0).label,
                score = predictionResults.get(0).score,
                NIS = "2210221312-021",
                age = 2,
                gender = cattleGender.MALE,
                isHealthy = true,
                location = "Kandang B2",
                race = "simmental",
                birth_date = "22-02-2020",
                parentM = "2210221312-019",
                parentF = "2210221312-005",
                owner = "Ridho ilahi"
            )
        } else {
            return null
        }
    }

    fun setImagePicked(uri: Uri?) {
        if (uri != null) {
            _imagePicked.value = uri
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveBitmap(
        bitmap: Bitmap?,
        contentResolver: ContentResolver,
        context: Context,
        writerPermissionGranted: Boolean
    ) {
        if (bitmap != null) {
            val isSuccessfullySaved = saveBitmapUtils(
                contentResolver = contentResolver,
                bitmap = bitmap,
                context = context,
                writePermissionGranted = writerPermissionGranted
            )
            if (isSuccessfullySaved) {
                loadPhotosFromExternalStorage(contentResolver = contentResolver).let {
                    Log.d("savebitmap", "saveBitmap: uri granted")
                    setImagePicked(it)
                }
            }
        } else {
            Log.e("savebitmap", "saveBitmap: bitmap is null")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveBitmapFromUri(uri: Uri?, contentResolver: ContentResolver, context: Context, writePermissionGranted: Boolean){
        val bitmap = uri?.let { uriToBitmap(it, context) }
        saveBitmap(bitmap, contentResolver, context, writePermissionGranted)
    }


    suspend fun mlPredictResultsFromUri(uri: Uri, context: Context) {
        val bitmap = uriToBitmap(uri, context = context)
        _predictionResults.value = predict(context, bitmap)
    }

    suspend fun uploadImage(context: Context) {
        val file: File = _imagePicked.value.getFile(context)!!
        val requestResult = uploadImageUtils(file)
        if (requestResult == 200) {
            Toast.makeText(context, "Berhasil mengupload gambar", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Gagal upload", Toast.LENGTH_SHORT).show()
        }
    }


}