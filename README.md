# Unique Person Collage

An Android application that analyzes a video, detects faces, identifies unique people using face embeddings, and selects representative frames for each person to create a collage.

## Features

* Select a video from the device
* Sample frames from the video
* Detect faces using Google ML Kit
* Generate face embeddings using an ONNX embedding model
* Group observations belonging to the same person
* Track separate appearances of each person
* Select the best representative frame for each person
* Rank frames using:

  * Face sharpness
  * Face frontality
  * Eye openness
  * Facial expression
  * Face visibility
* Generate a final collage from the selected representatives

## Tech Stack

* **Kotlin**
* **Android**
* **Jetpack Compose**
* **ML Kit Face Detection**
* **ONNX Runtime**
* **Coroutines**
* **AndroidX ViewModel**

## Project Structure

```text
app/
└── src/
    └── main/
        ├── java/com/example/collegeapp/
        │   ├── detection/
        │   │   ├── FaceDetector.kt
        │   │   └── FaceCropper.kt
        │   │
        │   ├── processing/
        │   │   ├── VideoProcessor.kt
        │   │   ├── IdentityClusterer.kt
        │   │   ├── AppearanceTracker.kt
        │   │   └── RepresentativeSelector.kt
        │   │
        │   ├── model/
        │   │   └── ...
        │   │
        │   └── ui/
        │       └── ...
        │
        └── assets/
            └── face_embedding.onnx
```

## Requirements

* Android Studio
* Android SDK
* JDK compatible with the project's Gradle/Android Gradle Plugin configuration
* Android device or emulator
* A device with sufficient memory for video processing and ONNX inference

## Setup

### 1. Clone the repository

```bash
git clone git@github.com:adithyanmanayil/UniquePersonCollage.git
cd UniquePersonCollage
```

### 2. Open the project

Open the project in Android Studio.

Allow Android Studio to:

* Sync Gradle
* Download required dependencies
* Install any missing Android SDK components

### 3. Add the embedding model

The application expects the face embedding model at:

```text
app/src/main/assets/face_embedding.onnx
```

The model is intentionally **not stored in GitHub** because its size exceeds GitHub's 100 MB single-file limit.

Place the model locally at:

```text
app/src/main/assets/face_embedding.onnx
```

The filename must remain exactly:

```text
face_embedding.onnx
```

The model file is ignored by Git and therefore will not be committed to the repository.

## Build

From Android Studio:

1. Open the project.
2. Wait for Gradle synchronization to complete.
3. Select an Android device or emulator.
4. Click **Run**.

Alternatively, build from the terminal:

```bash
./gradlew assembleDebug
```

The generated APK will be located under:

```text
app/build/outputs/apk/debug/
```

## Running the Application

1. Launch the application.
2. Select a video from the device.
3. The application samples frames from the video.
4. Faces are detected using ML Kit.
5. Face regions are processed by the embedding model.
6. Face embeddings are clustered into unique identities.
7. Appearances are tracked separately.
8. The strongest representative frame is selected for each person.
9. The final collage is displayed.

## Face Detection

Face detection is performed using **Google ML Kit Face Detection**.

The detector is configured with:

* Fast performance mode
* All landmarks
* All classifications
* Face tracking enabled
* Contours disabled

The detector extracts information including:

* Bounding box
* Tracking ID
* Head rotation
* Left eye openness
* Right eye openness
* Smiling probability

## Face Embedding Model

The application uses the following local ONNX model:

```text
face_embedding.onnx
```

The model generates a numerical embedding representing the detected face.

These embeddings are used for identity clustering rather than relying solely on ML Kit tracking IDs.

The embedding model is loaded locally by the application through ONNX Runtime.

> The model file is excluded from the Git repository because it is approximately 130 MB and exceeds GitHub's 100 MB file-size limit.

## Identity Matching

Face identities are determined using **cosine similarity** between face embeddings.

The selected similarity threshold is:

```text
0.75
```

### Threshold

```kotlin
const val DEFAULT_SIMILARITY_THRESHOLD = 0.75f
```

An observation is assigned to an existing identity when its embedding has a cosine similarity of at least `0.75` with the cluster centroid.

Conceptually:

```text
similarity >= 0.75
        ↓
same identity
```

Otherwise, a new identity cluster is created.

The cluster centroid is updated as additional observations are assigned to the identity.

## Why 0.75?

A threshold of **0.75** was selected as the working threshold for separating distinct people while allowing embeddings of the same person to remain in the same cluster despite changes in pose, expression, and frame quality.

The threshold is configurable and can be adjusted in:

```text
processing/IdentityClusterer.kt
```

## Representative Frame Selection

For each detected face observation, the application calculates several quality scores.

### Sharpness — 30%

Measures the amount of high-frequency detail in the face region using a lightweight Laplacian-style calculation.

### Frontality — 25%

Uses:

* Head pitch
* Head yaw
* Head roll

Smaller deviations from a frontal pose receive higher scores.

### Eyes — 20%

Uses ML Kit eye-open probabilities.

Frames with more open eyes receive higher scores.

### Expression — 10%

Uses ML Kit smiling probability.

A neutral expression receives a baseline score while a pleasant expression receives a higher score.

### Face Visibility — 15%

Considers:

* Whether the face is clipped by the image boundary
* Relative face size
* Amount of visible face

The final representative score is:

```text
30% sharpness
+ 25% frontality
+ 20% eye openness
+ 10% expression
+ 15% face visibility
```

## Identity vs Appearance

The application separates **identity** from **appearance**.

### Identity

Determines:

> "Is this the same person?"

This is based primarily on face embedding similarity.

### Appearance

Determines:

> "Is this a separate occurrence of the person in the video?"

This prevents a person who appears multiple times in different parts of a video from being incorrectly treated as a completely new identity.

## Large Model Handling

The embedding model is excluded from Git:

```gitignore
app/src/main/assets/face_embedding.onnx
```

This keeps the Git repository within GitHub's file-size limits.

Before building the application, ensure the model exists locally:

```bash
ls -lh app/src/main/assets/face_embedding.onnx
```

## Troubleshooting

### `face_embedding.onnx` not found

Make sure the file exists at:

```text
app/src/main/assets/face_embedding.onnx
```

Then rebuild the application.

### Gradle build errors

Try:

```bash
./gradlew clean
./gradlew assembleDebug
```

Then synchronize the project again in Android Studio.

### Application closes during processing

Check Logcat for:

```text
FATAL EXCEPTION
```

The relevant exception and stack trace will normally identify the failing processing stage.

## Git Development

The embedding model is intentionally ignored:

```bash
git status
```

should not show:

```text
app/src/main/assets/face_embedding.onnx
```

To verify that the model is not present in Git history:

```bash
git rev-list --objects --all | grep face_embedding.onnx
```

This command should return no output.

## License

Add the project's license information here if applicable.
