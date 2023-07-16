package com.bagih.sis

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.bagih.sis.presentation.MainViewModel
import com.bagih.sis.ui.layout.component.*
import com.bagih.sis.ui.theme.SISTheme
import com.bagih.sis.utils.uriToBitmap
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    val viewModel: MainViewModel = MainViewModel()
    private val TAG = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.Q)
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // Use the returned uri.
            val uriContent = result.uriContent

            viewModel.viewModelScope.launch {
                viewModel.saveBitmapFromUri(uriContent, contentResolver, baseContext, writePermissionGranted)
            }
            // optional usage
        } else {
            // An error occurred.
            val exception = result.error
            Log.e("cropImage", ": result unsucessfully: $exception")
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                readPermissionGranted =
                    permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                        ?: readPermissionGranted
                writePermissionGranted =
                    permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE]
                        ?: writePermissionGranted


            }
        updateOrRequestPermissions(context = applicationContext)

        setContent {
            SISTheme {
                val imagePicked by viewModel.imagePicked.collectAsState()
                val predictionResults by viewModel.predictionResults.collectAsState()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Scaffold(
                        topBar = {
                            SISTopAppBar(title = "Sistem Identifikasi Sapi", uploadButtonAction = {
                                try {
                                    viewModel.viewModelScope.launch {
                                        viewModel.uploadImage(baseContext)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                startCrop(imagePicked)
                            }
                        },
                        floatingActionButton = {
                            SISFab(fabButtonOnResult = {
                                viewModel.viewModelScope.launch {
                                    viewModel.saveBitmap(
                                        bitmap = it,
                                        contentResolver = contentResolver,
                                        context = baseContext,
                                        writerPermissionGranted = writePermissionGranted
                                    )
                                }
                            })
                        },
                        floatingActionButtonPosition = FabPosition.Center,
                        isFloatingActionButtonDocked = true,
                        bottomBar = { SISBottomAppBar() }
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = imagePicked,
                                contentDescription = "image of cattle muzzle",
                                modifier = Modifier
                                    .height(200.dp)
                                    .padding(top = it.calculateTopPadding() + 8.dp)
                            )
                            if (predictionResults.isNotEmpty()) {
                                val cattleBio = viewModel.getCattleBio(predictionResults)
                                Log.d(
                                    "result",
                                    "${predictionResults.get(0).label} : ${predictionResults.get(0).score}"
                                )
                                if(predictionResults.get(0).score <= 0.2){
                                    Text(
                                        text = "Objek bukan sapi",
                                        modifier = Modifier.padding(top = it.calculateTopPadding() + 8.dp)
                                    )
                                }
                                else if (predictionResults.get(0).score <= 0.5) {
                                    Text(
                                        text = "Sapi tidak terdaftar dalam database",
                                        modifier = Modifier.padding(top = it.calculateTopPadding() + 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Hasil Prediksi")
                                    for (i in 0..2){
                                        TableRow(label = predictionResults[i].label, value = predictionResults[i].score.toString())
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = it.calculateTopPadding() + 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Hasil Prediksi")
                                        for (i in 0..2){
                                            TableRow(label = predictionResults[i].label, value = predictionResults[i].score.toString())
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Keterangan Sapi")
                                        if (cattleBio != null) {
                                            TableRow(
                                                label = "Skor",
                                                value = cattleBio.score.toString()
                                            );
                                            TableRow(label = "nama", value = cattleBio.name);
                                            TableRow("NIS", cattleBio.NIS);
                                            TableRow(label = "Ras", value = cattleBio.race);
                                            TableRow(
                                                label = "umur",
                                                value = cattleBio.age.toString()
                                            );
                                            TableRow(
                                                label = "gender",
                                                value = cattleBio.gender.toString()
                                            );
                                            TableRow(label = "kandang", value = cattleBio.location);
                                            TableRow(
                                                label = "kelahiran",
                                                value = cattleBio.birth_date
                                            );
                                            TableRow(label = "induk jantan", value = cattleBio.parentM);
                                            TableRow(label = "induk betina", value = cattleBio.parentF);
                                            TableRow(label = "pemilik", value = cattleBio.owner);
                                            when (cattleBio.isHealthy) {
                                                true -> TableRow(
                                                    label = "kesehatan",
                                                    value = "sehat"
                                                )
                                                false -> TableRow(
                                                    label = "kesehatan",
                                                    value = "sakit"
                                                )
                                            }
                                            Spacer(modifier = Modifier.padding(bottom = it.calculateBottomPadding() + 8.dp))
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "lakukan prediksi untuk melihat hasil",
                                    modifier = Modifier.padding(top = it.calculateTopPadding() + 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun updateOrRequestPermissions(context: Context) {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest = mutableListOf<String>()
        if (!writePermissionGranted) {
            permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!readPermissionGranted) {
            permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun SISBottomAppBar() {
        val imagePicked by viewModel.imagePicked.collectAsState()
        val singleImagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                viewModel.viewModelScope.launch {
                    if (uri != null) {
                        viewModel.setImagePicked(uri)
                    }
                }
            }
        )

        BottomAppBar(
            cutoutShape = MaterialTheme.shapes.small.copy(
                CornerSize(50)
            ),
        )
        {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(
                    onClick = {
                        singleImagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_baseline_image_24),
                        contentDescription = "pick an image"
                    )
                }
                IconButton(onClick = {
                    viewModel.viewModelScope.launch {
                        viewModel.mlPredictResultsFromUri(imagePicked, baseContext)
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_round_check_circle_24),
                        contentDescription = "reload data"
                    )
                }
            }
        }
    }

    private fun startCrop(uri: Uri) {
        // Start picker to get image for cropping and then use the image in cropping activity.
        cropImage.launch(
            CropImageContractOptions(
                uri = uri,
                cropImageOptions = CropImageOptions(
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true,
                    guidelines = CropImageView.Guidelines.ON,
                    cropShape = CropImageView.CropShape.RECTANGLE,
                    outputCompressFormat = Bitmap.CompressFormat.PNG
                )
            )
        )

    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SISTheme {
//        MainPage()
    }
}