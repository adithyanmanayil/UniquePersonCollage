package com.example.uniquepersoncollage

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContent {
            UniquePersonCollageApp()
        }
    }

}

@Composable
fun UniquePersonCollageApp() {

    MaterialTheme {

        Surface(
            modifier =
                Modifier.fillMaxSize(),
            color =
                Color(0xFFF4F8FF)
        ) {

            VideoPickerScreen()
        }
    }

}

@Composable
fun VideoPickerScreen() {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var selectedVideo by remember {
        mutableStateOf<Uri?>(null)
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var progress by remember {

        mutableStateOf(
            ProcessingProgress(
                processedFrames = 0,
                totalFrames = 0,
                facesFound = 0
            )
        )
    }

    var processedFaces by remember {
        mutableStateOf<List<ProcessedFace>>(
            emptyList()
        )
    }

    var personClusters by remember {
        mutableStateOf<List<PersonCluster>>(
            emptyList()
        )
    }

    var representativeShots by remember {
        mutableStateOf<List<RepresentativeShot>>(
            emptyList()
        )
    }

    var collageBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val picker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                selectedVideo = uri

                processedFaces =
                    emptyList()

                personClusters =
                    emptyList()

                representativeShots =
                    emptyList()

                collageBitmap =
                    null

                progress =
                    ProcessingProgress(
                        processedFrames = 0,
                        totalFrames = 0,
                        facesFound = 0
                    )
            }
        }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 28.dp
                )
    ) {

        /*
         * HEADER
         */

        Text(
            text = "Unique Person",
            fontSize = 28.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                Color(0xFF102A43)
        )

        Text(
            text =
                "Create a collage from your video",
            fontSize = 15.sp,
            color =
                Color(0xFF627D98),
            modifier =
                Modifier.padding(
                    top = 4.dp
                )
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        /*
         * VIDEO
         */

        if (selectedVideo == null) {

            EmptyVideoCard(
                onChooseVideo = {
                    picker.launch("video/*")
                }
            )

        } else {

            VideoPreviewCard(
                videoUri =
                    selectedVideo!!,
                enabled =
                    !isProcessing
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            /*
             * CHOOSE ANOTHER VIDEO
             */

            Button(
                enabled =
                    !isProcessing,

                onClick = {
                    picker.launch("video/*")
                },

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFE4EEFF),
                        contentColor =
                            Color(0xFF1769AA)
                    )
            ) {

                Text(
                    text =
                        "Choose another video",
                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            /*
             * PROCESS BUTTON
             */

            Button(
                enabled =
                    !isProcessing,

                onClick = {

                    val videoUri =
                        selectedVideo
                            ?: return@Button

                    isProcessing =
                        true

                    scope.launch {

                        try {

                            val results =
                                VideoProcessor(
                                    context
                                ).process(
                                    uri = videoUri,
                                    onProgress = {
                                            newProgress ->

                                        progress =
                                            newProgress
                                    }
                                )

                            processedFaces =
                                results

                            personClusters =
                                PersonClusterer(
                                    similarityThreshold =
                                        0.40f
                                ).cluster(
                                    results
                                )

                            representativeShots =
                                RepresentativeShotSelector()
                                    .select(
                                        personClusters
                                    )

                            collageBitmap =
                                CollageGenerator
                                    .createCollage(
                                        representativeShots
                                    )

                        } catch (
                            error: Exception
                        ) {

                            error.printStackTrace()

                        } finally {

                            isProcessing =
                                false
                        }
                    }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF1976D2),
                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        if (isProcessing) {
                            "Creating collage..."
                        } else {
                            "Create Collage"
                        },

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        /*
         * PROCESSING
         */

        if (isProcessing) {

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            ProcessingCard(
                progress =
                    progress
            )
        }


        /*
         * RESULTS
         */

        if (
            !isProcessing &&
            processedFaces.isNotEmpty()
        ) {

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            ResultSummaryCard(
                faces =
                    processedFaces.size,
                people =
                    personClusters.size
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            collageBitmap?.let { bitmap ->

                CollageCard(
                    bitmap =
                        bitmap
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )
    }

}

/*

* =================================================
* EMPTY VIDEO CARD
* =================================================
  */

@Composable
private fun EmptyVideoCard(
    onChooseVideo: () -> Unit
) {

    GlassCard {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(28.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(76.dp)
                        .clip(
                            RoundedCornerShape(24.dp)
                        )
                        .background(
                            Color(0xFFE1ECFF)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "▶",
                    fontSize = 28.sp,
                    color =
                        Color(0xFF1976D2)
                )
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            Text(
                text =
                    "Choose a video",
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    Color(0xFF102A43)
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            Text(
                text =
                    "Select a portrait video to find all distinct people.",
                fontSize =
                    14.sp,
                color =
                    Color(0xFF627D98)
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            Button(
                onClick =
                    onChooseVideo,

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF1976D2)
                    )
            ) {

                Text(
                    text =
                        "Select Video",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }

}

/*

* =================================================
* VIDEO PREVIEW
* =================================================
  */

@Composable
private fun VideoPreviewCard(
    videoUri: Uri,
    enabled: Boolean
) {

    GlassCard {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
        ) {

            VideoPlayer(
                videoUri =
                    videoUri,
                enabled =
                    enabled
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 6.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(9.dp)
                            .clip(
                                RoundedCornerShape(
                                    50
                                )
                            )
                            .background(
                                Color(0xFF1976D2)
                            )
                )


                Spacer(
                    modifier =
                        Modifier.width(9.dp)
                )


                Text(
                    text =
                        "Video ready",
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        Color(0xFF243B53)
                )
            }
        }
    }

}

/*

* =================================================
* BUILT-IN ANDROID VIDEO PLAYER
* =================================================
  */

@Composable
private fun VideoPlayer(
    videoUri: Uri,
    enabled: Boolean
) {

    val context =
        LocalContext.current

    val videoView =
        remember(videoUri) {

            VideoView(
                context
            ).apply {

                setVideoURI(
                    videoUri
                )

                setMediaController(
                    MediaController(
                        context
                    ).apply {
                        setAnchorView(this@apply)
                    }
                )

                setOnPreparedListener {
                    it.isLooping = false
                }
            }
        }


    DisposableEffect(
        videoUri
    ) {

        onDispose {

            videoView.stopPlayback()
        }
    }


    AndroidView(
        factory = {
            videoView
        },

        update = {

            it.isEnabled =
                enabled
        },

        modifier =
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .clip(
                    RoundedCornerShape(20.dp)
                )
    )
}

/*

* =================================================
* PROCESSING CARD
* =================================================
  */

@Composable
private fun ProcessingCard(
    progress: ProcessingProgress
) {

    GlassCard {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
        ) {

            Text(
                text =
                    "Analyzing video",
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    Color(0xFF102A43)
            )


            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )


            Text(
                text =
                    "Detecting and grouping distinct people...",
                fontSize =
                    13.sp,
                color =
                    Color(0xFF627D98)
            )


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            LinearProgressIndicator(
                progress = {

                    (
                            progress.percent /
                                    100f
                            ).coerceIn(
                            0f,
                            1f
                        )
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    text =
                        "${progress.percent}%",

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF1976D2)
                )

                Text(
                    text =
                        "${progress.facesFound} faces",

                    color =
                        Color(0xFF627D98)
                )
            }
        }
    }

}

/*

* =================================================
* RESULT SUMMARY
* =================================================
  */

@Composable
private fun ResultSummaryCard(
    faces: Int,
    people: Int
) {

    GlassCard {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            StatItem(
                value =
                    faces.toString(),
                label =
                    "Faces"
            )


            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(
                            Color(0xFFD9E2EC)
                        )
            )


            StatItem(
                value =
                    people.toString(),
                label =
                    "Unique People"
            )
        }
    }

}

@Composable
private fun StatItem(
    value: String,
    label: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                value,

            fontSize =
                25.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                Color(0xFF1976D2)
        )


        Spacer(
            modifier =
                Modifier.height(3.dp)
        )


        Text(
            text =
                label,

            fontSize =
                12.sp,

            color =
                Color(0xFF627D98)
        )
    }
}

/*

* =================================================
* COLLAGE CARD
* =================================================
  */

@Composable
private fun CollageCard(
    bitmap: Bitmap
) {

    GlassCard {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
        ) {

            Text(
                text =
                    "Your Collage",

                modifier =
                    Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 6.dp
                    ),

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color(0xFF102A43)
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Image(
                bitmap =
                    bitmap.asImageBitmap(),

                contentDescription =
                    "Generated collage",

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(18.dp)
                        ),

                contentScale =
                    ContentScale.Fit
            )
        }
    }

}

/*

* =================================================
* GLASS CARD
* =================================================
  */

@Composable
private fun GlassCard(
    content: @Composable () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(26.dp)
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(
                                0xFFEAF4FF
                            ).copy(
                                alpha = 0.92f
                            ),

                            Color(
                                0xFFDDEBFA
                            ).copy(
                                alpha = 0.82f
                            )
                        )
                    )
                )
                .border(
                    width =
                        1.dp,

                    color =
                        Color.White.copy(
                            alpha = 0.85f
                        ),

                    shape =
                        RoundedCornerShape(26.dp)
                )
    ) {

        content()
    }

}
