package io.soft.imagenee.presentation.main

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import io.soft.imagenee.BuildConfig
import io.soft.imagenee.data.model.Image
import io.soft.imagenee.helper.createFile
import io.soft.imagenee.helper.getBitmapFromUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainVM = hiltViewModel(),
    onOpenImage: (Image) -> Unit,
    onLogOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val context = LocalContext.current
    val deleteMode by viewModel.deleteMode.collectAsState()

    var uploading by remember { mutableStateOf(false) }
    var showImageUploadDialog by remember { mutableStateOf(false) }
    var imageTitle by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            val uri = it ?: return@rememberLauncherForActivityResult
            imageUri = uri
            showImageUploadDialog = true
        }
    )

    LaunchedEffect(key1 = Unit) {
        viewModel
            .effect
            .collect {
                when (it) {
                    is UIEffect.FileUploadFailure -> Toast.makeText(
                        context,
                        it.failureReason,
                        Toast.LENGTH_SHORT
                    ).show()

                    UIEffect.FileUploading -> uploading = true
                    UIEffect.Processed -> uploading = false
                    UIEffect.LogOut -> onLogOut()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MainAppBar(
            onLogOut = viewModel::logOut,
            user = userName
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val currentState = state) {
                ImagesState.Loading -> CircularProgressIndicator()
                ImagesState.NotLoading -> {}
                is ImagesState.Success -> Success(
                    currentState.images,
                    onImageSelected = viewModel::select,
                    onOpenImage = {
                        if (deleteMode != null) {
                            viewModel.select(it.id)
                        } else {
                            onOpenImage(it)
                        }
                    }
                )

                is ImagesState.Error -> Text(text = currentState.toHumanMessage())
            }

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                onClick = {
                    if (uploading.not()) {
                        if (deleteMode != null) {
                            viewModel.deleteImage()
                        } else {
                            launcher.launch("image/*")
                        }
                    }
                },
            ) {
                if (uploading.not()) {
                    Icon(
                        imageVector = if (deleteMode != null) Icons.Default.Delete else Icons.Default.Add,
                        contentDescription = null
                    )
                } else
                    CircularProgressIndicator(modifier = Modifier.scale(0.5f))
            }

            if (showImageUploadDialog) {
                Dialog(
                    onDismissRequest = { showImageUploadDialog = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = "Enter image title",
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.Center),
                            textAlign = TextAlign.Center,
                        )

                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            value = imageTitle,
                            onValueChange = {
                                imageTitle = it
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Absolute.SpaceEvenly
                        ) {
                            Button(onClick = {
                                imageTitle = ""
                                showImageUploadDialog = false
                            }) {
                                Text(text = "Cancel")
                            }

                            Button(onClick = {
                                imageUri ?: return@Button
                                val bitmap = getBitmapFromUri(context.contentResolver, imageUri!!)
                                val file = bitmap!!.createFile(context)
                                viewModel.uploadImage(
                                    file,
                                    imageTitle = imageTitle,
                                    userId = ""
                                )
                                imageTitle = ""
                                showImageUploadDialog = false
                            }) {
                                Text(text = "Upload")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    onLogOut: () -> Unit,
    user: String?
) {
    TopAppBar(
        title = {
            Text(
                text = user ?: "User",
                fontSize = 18.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onLogOut) {
                Icon(imageVector = Icons.Filled.Logout, contentDescription = null)
            }
        }
    )
}

@Composable
fun Success(
    images: List<Image>?,
    onImageSelected: (String) -> Unit,
    onOpenImage: (Image) -> Unit
) {
    images ?: return
    LazyVerticalGrid(
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2)
    ) {
        items(images) { image ->
            ImageItem(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp),
                image = image,
                onSelected = {
                    onImageSelected(image.id)
                },
                onOpenImage = {
                    onOpenImage(image)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageItem(
    modifier: Modifier,
    image: Image,
    onSelected: () -> Unit,
    onOpenImage: () -> Unit
) {
    val painter =
        rememberAsyncImagePainter(model = BuildConfig.BASE_URL + "../static/${image.image}")
    Box(modifier = modifier.combinedClickable(
        onClick = {
            if (image.isSelected) {
                onSelected()
            } else {
                onOpenImage()
            }
        },
        onLongClick = {
            onSelected()
        }
    )) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier.fillMaxSize(1f),
            contentScale = ContentScale.Crop
        )

        if (image.isSelected) {
            Icon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null
            )
        }
    }
}