package com.bagih.sis.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.bagih.sis.ml.Model
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

fun predict(context: Context, bitmap: Bitmap): List<Category> {
    val rescaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, false)
    val convertedBitmap = rescaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
    bitmap.recycle()
    rescaledBitmap.recycle()
    val model = Model.newInstance(context)

// Creates inputs for reference.
    val image = TensorImage.fromBitmap(convertedBitmap)
//    val image = TensorBuffer.
// Runs model inference and gets result.
    val outputs = model.process(image)
    val probability = outputs.probabilityAsCategoryList.sortedByDescending { it.score }
//    Log.d("res", "predict: $outputs")

// Releases model resources if no longer used.
    model.close()
    return probability
}

suspend fun uriToBitmap(uri: Uri, context: Context): Bitmap {
    val loading: ImageLoader = ImageLoader(context)
    val request: ImageRequest = ImageRequest.Builder(context)
        .data(uri)
        .build()

    val result: Drawable = (loading.execute(request) as SuccessResult).drawable
    return (result as BitmapDrawable).bitmap
}