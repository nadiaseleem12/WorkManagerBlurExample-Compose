package com.example.workmanagerexample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BlurWorker(appContext: Context, params: WorkerParameters) :CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        //String -> Uri -> stream -> Bitmap

        return  withContext(Dispatchers.IO){
            try {

                val resourceUriString = inputData.getString(KEY_IMAGE_URI)
                Log.e("TAG", "resourceUriString: ${resourceUriString.toString()}" )

                val blurLevel = inputData.getInt(KEY_BLUR_LEVEL,1)
                val uri = Uri.parse(resourceUriString)
                val imageInputStream = applicationContext.contentResolver.openInputStream(uri)

                val bitmap = BitmapFactory.decodeStream(imageInputStream)

                val outputBitmap = blurImage(bitmap,blurLevel)

                val outputUri = saveBitmapToFile(outputBitmap)

                Result.success(workDataOf(KEY_IMAGE_URI to outputUri.toString()))

            }catch (t:Throwable){
                Log.e("TAG", "doWork: $t", )
                Result.failure(workDataOf(KEY_ERROR to t.localizedMessage))
            }

        }

    }

    private fun saveBitmapToFile(outputBitmap: Bitmap): Uri {
        val fileName = "blurred_image.png"
        val outputDir = applicationContext.cacheDir
        val outputFile = File(outputDir,fileName)
        FileOutputStream(outputFile).use {outputStram->
            outputBitmap.compress(Bitmap.CompressFormat.PNG,100,outputStram)
        }
       return Uri.fromFile(outputFile)

    }

    private fun blurImage(bitmap: Bitmap,blurLevel:Int): Bitmap {
        val input =  Bitmap.createScaledBitmap(
            bitmap,bitmap.width/(blurLevel*5),
            bitmap.height/(blurLevel*5),
            true
        )

        return  Bitmap.createScaledBitmap(input,bitmap.width,bitmap.height,true)
    }

    companion object{
        const val KEY_IMAGE_URI = "image uri"
        const val KEY_BLUR_LEVEL = "blur level"
        const val KEY_ERROR = "error"
    }
}