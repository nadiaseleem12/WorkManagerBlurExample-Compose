package com.example.workmanagerexample

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import coil3.compose.rememberAsyncImagePainter
import com.example.workmanagerexample.ui.theme.WorkManagerExampleTheme
import java.util.UUID
import kotlin.math.log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkManagerExampleTheme {
                HomeScreen(applicationContext)
            }
        }
    }
}


@Composable
fun HomeScreen(context: Context) {
    val inputImageUri = getImageUri()
    val workManager = remember {
        WorkManager.getInstance(context)
    }
    var worRequestId by remember { mutableStateOf<UUID?>(null) }
    var outputImageUri by remember { mutableStateOf<Uri?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // First Image (Before Blurring)
        Image(
            painter = painterResource(id = R.drawable.android_cupcake),
            contentDescription = "Cupcake image",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Crop
        )

        // Button to Apply Blur
        Button(onClick = {
           val blurWorkRequest = createBlurWorkRequest(inputImageUri)
            workManager.enqueue(blurWorkRequest)
            worRequestId =  blurWorkRequest.id

        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Apply Blur")
        }

        outputImageUri?.let {uri->
            Image(
                painter =rememberAsyncImagePainter(uri) ,
                contentDescription = "Cupcake image after blurring",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
        }

    }

    worRequestId?.let {
        val workInfoState = workManager.getWorkInfoByIdFlow(it).collectAsState(null)

        workInfoState.value?.let {workInfo ->
            when(workInfo.state){

                WorkInfo.State.SUCCEEDED ->{
                    val data = workInfo.outputData.getString(BlurWorker.KEY_IMAGE_URI)
                    outputImageUri =Uri.parse(data)
                }
                WorkInfo.State.FAILED ->{
                    Toast.makeText(LocalContext.current,workInfo.outputData.getString(BlurWorker.KEY_ERROR),Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }

        }
    }

}

fun createBlurWorkRequest(inputImageUri: Uri): WorkRequest {
val data = Data.Builder()
    .putString(BlurWorker.KEY_IMAGE_URI,inputImageUri.toString())
    .putInt(BlurWorker.KEY_BLUR_LEVEL,3)
    .build()
    Log.e("TAG", "inputImageUri: ${inputImageUri.toString()}" )
    return   OneTimeWorkRequestBuilder<BlurWorker>()
        .setInputData(data)
        .build()

}

@Composable
fun getImageUri(): Uri {
    val context = LocalContext.current
//    return  Uri.parse(
//        "android.resource://${context.packageName}/drawable/android_cupcake"
//    )
    return  Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(R.drawable.android_cupcake))
        .appendPath(context.resources.getResourceTypeName(R.drawable.android_cupcake))
        .appendPath(context.resources.getResourceEntryName(R.drawable.android_cupcake))
        .build()
}
