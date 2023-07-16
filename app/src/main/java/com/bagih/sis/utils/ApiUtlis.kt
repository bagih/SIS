package com.bagih.sis.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.util.cio.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

suspend fun uploadImage(file: File): Int{
    val client = HttpClient(CIO)
    val response = client.put("https://tvekiekdk0.execute-api.ap-southeast-3.amazonaws.com/dev/muzzle-ident-dataset/"+file.name){
        headers {
            append("x-api-key", "0aQBCtlj9i32gNqwLSWWZ5jESTmyEfwr5kslhbuK")
        }
        setBody(file.readChannel())
    }
    Log.d(ContentValues.TAG, "uploadImage: ${response.status.value}")
    client.close()
    return response.status.value
}

// extension functions
fun Uri.getRealName (context: Context): String?
{
    val cursor = context.contentResolver.query(this, null, null, null, null)
    if (cursor == null || !cursor.moveToFirst()) return null

    val indexName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val realName = cursor.getString(indexName)
    cursor.close()

    return realName
}

fun Uri.getFile (context: Context): File?
{
    val fileDescriptor = context.contentResolver.openFileDescriptor(this, "r", null)
    if (fileDescriptor == null) return null

    val file = File(context.cacheDir, getRealName(context)!!)
    val fileOutputStream = FileOutputStream(file)

    val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
    fileInputStream.copyTo(fileOutputStream)
    fileDescriptor.close()

    return file
}

fun File.getMimeType (): String?
{
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}