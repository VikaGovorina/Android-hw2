package com.example.hw2_gif_api

import android.content.res.Configuration
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.hw2_gif_api.ui.theme.HW2_GIF_apiTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    private val retrofitController by lazy { RetrofitController() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HW2_GIF_apiTheme {
                MainScreen(retrofitController)
            }
        }
    }

    @Composable
    fun MainScreen(retrofitController: RetrofitController) {
        val gifSaver: Saver<MutableList<GifData>, String> = Saver(
            save = {gifs ->
                Json.encodeToString(gifs)
            },
            restore = {jsonString ->
                Json.decodeFromString<MutableList<GifData>>(jsonString)
            }
        )

        val scope = rememberCoroutineScope()
        var state by remember { mutableStateOf<State>(State.Ok(Gifs(data = emptyList()))) }
        var offset by rememberSaveable { mutableIntStateOf(0) }
        var gifs = rememberSaveable(saver = gifSaver) { mutableStateListOf<GifData>() }
        val handler = CoroutineExceptionHandler { _, exception ->
            state = State.Error(exception.message.toString())

        }

        fun loadGifs(newOffset: Int) {
            scope.launch(handler) {
                state = State.Loading
                try {
                    val result = retrofitController.requestGifs(newOffset)
                    if (result is State.Ok) {
                        val newGifs = result.gifs.data.filterNot { existingGif ->
                            gifs.any { it.id == existingGif.id }
                        }
                        gifs.addAll(newGifs)
//                        gifs.addAll(result.gifs.data)
                        offset += 20
                    }
                    state = result
                } catch (e: Exception) {
                    state = State.Error("error")
                }

            }
        }

        if (state is State.Ok && gifs.isEmpty()) {
            loadGifs(offset)
        }

        Column (modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp)) {
            when (state) {
                is State.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is State.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                scope.launch(handler) {
                                    state = State.Loading
                                    try {
                                        val result = retrofitController.requestGifs(offset)
                                        if (result is State.Ok) {
                                            gifs.addAll(result.gifs.data)
                                            offset += 20
                                        }
                                        state = result
                                    } catch (e: Exception) {
                                        state = State.Error(e.message.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getString(R.string.retry),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                }

                is State.Ok -> {
                    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {

                        LazyVerticalGrid (
                            columns = GridCells.Fixed(4),
                        ) {
                            items(gifs) { gif ->
                                GifItem(gif)
                            }
                        }
                    } else {
                        LazyColumn {
                            items(gifs) { gif ->
                                GifItem(gif)
                            }

                        }
                    }
                }
            }
            if (state is State.Ok) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gifs) { gif ->
                        GifItem(gif)
                    }
                }

                LaunchedEffect(listState.firstVisibleItemIndex) {
                    val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                    if (lastVisibleItem != null && lastVisibleItem.index == gifs.size - 1) {
                        loadGifs(offset)
                    }
                }
            }


        }


    }

    @Composable
    fun GifItem(gifData: GifData) {

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                modifier = Modifier
                    .fillMaxSize(),
                update = {imageView ->
                    Glide.with(imageView.context)
                        .load(gifData.images.original.url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView)
                }
            )
        }
    }

}
