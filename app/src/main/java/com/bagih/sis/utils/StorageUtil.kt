package com.bagih.sis.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bagih.sis.data.SharedStoragePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

inline fun <T> sdk29AndUp(onSdk29: () -> T): T? {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        onSdk29()
    } else null
}

fun savePhotoToExternalStorage(contentResolver: ContentResolver ,displayName: String, bmp: Bitmap): Boolean {
    val imageCollection = sdk29AndUp {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.WIDTH, bmp.width)
        put(MediaStore.Images.Media.HEIGHT, bmp.height)
    }
    return try {
        contentResolver.insert(imageCollection, contentValues)?.also { uri ->
            contentResolver.openOutputStream(uri).use { outputStream ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                    throw IOException("Couldn't save bitmap")
                }
            }
        } ?: throw IOException("Couldn't create MediaStore entry")
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

fun saveBitmap(contentResolver: ContentResolver, writePermissionGranted: Boolean, context: Context, bitmap: Bitmap?) : Boolean{

    if (bitmap == null) {
        Log.d("saveBitmap()", "saveBitmap: bitmap is null")
        return false
    } else {
        val isSavedSuccessfully = when {
            writePermissionGranted -> savePhotoToExternalStorage(
                contentResolver,
                UUID.randomUUID().toString(),
                bitmap
            )
            else -> false
        }
        if (isSavedSuccessfully) {
            Toast.makeText(context, "Photo saved successfully", Toast.LENGTH_SHORT).show()
            return true
        } else {
            Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
suspend fun loadPhotosFromExternalStorage(contentResolver: ContentResolver): Uri? {
    return withContext(Dispatchers.IO) {
        val collection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Pictures/") // Test was my folder name
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        val photos = mutableListOf<SharedStoragePhoto>()
        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                photos.add(SharedStoragePhoto(id, displayName, width, height, contentUri))
            }
            Log.d(
                "load photos",
                "loadPhotosFromExternalStorage: size : ${photos.toList().size}} content uri = ${
                    photos.toList().last().contentUri
                }"
            )
            photos.toList().last().contentUri
        } ?: null
    }
}