package fr.triquet.manyinone.loyalty

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import fr.triquet.manyinone.data.local.LoyaltyCard
import fr.triquet.manyinone.scanner.BarcodeAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    initialValue: String? = null,
    initialFormat: String? = null,
    editCardId: Long? = null,
    onBack: () -> Unit,
    viewModel: LoyaltyCardsViewModel = viewModel(),
) {
    val isEditing = editCardId != null

    var name by remember { mutableStateOf("") }
    var barcodeValue by remember { mutableStateOf(initialValue ?: "") }
    var barcodeFormat by remember { mutableStateOf(initialFormat ?: BarcodeFormatMapper.displayNames.first()) }
    var selectedColor by remember { mutableIntStateOf(LoyaltyCard.DEFAULT_COLOR) }
    var showScanner by remember { mutableStateOf(false) }
    var existingCard by remember { mutableStateOf<LoyaltyCard?>(null) }
    var isGalleryScanning by remember { mutableStateOf(false) }
    var galleryError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isGalleryScanning = true
        galleryError = null
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (_: Exception) {
            isGalleryScanning = false
            galleryError = "Impossible de lire l'image"
            return@rememberLauncherForActivityResult
        }
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.let { barcode ->
                    barcodeValue = barcode.rawValue ?: ""
                    barcodeFormat = BarcodeFormatMapper.fromMlKit(barcode.format)
                    galleryError = null
                } ?: run {
                    galleryError = "Aucun code-barres trouvé dans l'image"
                }
            }
            .addOnFailureListener {
                galleryError = "Erreur lors de la lecture de l'image"
            }
            .addOnCompleteListener {
                isGalleryScanning = false
                scanner.close()
            }
    }

    if (isEditing) {
        LaunchedEffect(editCardId) {
            viewModel.getById(editCardId!!)?.let { card ->
                existingCard = card
                name = card.name
                barcodeValue = card.barcodeValue
                barcodeFormat = card.barcodeFormat
                selectedColor = card.color
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Card" else "Add Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Card name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                label = { Text("Barcode value") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            FormatDropdown(
                selected = barcodeFormat,
                onSelected = { barcodeFormat = it },
            )

            Text(
                text = "Color",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            ColorPicker(
                selected = selectedColor,
                onSelected = { selectedColor = it },
            )

            if (showScanner && hasCameraPermission) {
                EmbeddedScanner(
                    onScanned = { value, format ->
                        barcodeValue = value
                        barcodeFormat = format
                        showScanner = false
                    },
                )
            } else {
                OutlinedButton(
                    onClick = {
                        if (hasCameraPermission) {
                            showScanner = true
                        } else {
                            hasCameraPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCameraPermission) showScanner = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("Scan barcode")
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isGalleryScanning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isGalleryScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("Pick from gallery")
                }
                galleryError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isEditing) {
                        existingCard?.let { card ->
                            viewModel.updateCard(
                                card.copy(
                                    name = name,
                                    barcodeValue = barcodeValue,
                                    barcodeFormat = barcodeFormat,
                                    color = selectedColor,
                                )
                            )
                        }
                    } else {
                        viewModel.addCard(name, barcodeValue, barcodeFormat, selectedColor)
                    }
                    onBack()
                },
                enabled = name.isNotBlank() && barcodeValue.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditing) "Update card" else "Save card")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private val cardColors = listOf(
    0xFF6750A4.toInt(), // Purple
    0xFFD32F2F.toInt(), // Red
    0xFFC2185B.toInt(), // Pink
    0xFF1976D2.toInt(), // Blue
    0xFF0097A7.toInt(), // Teal
    0xFF388E3C.toInt(), // Green
    0xFFF57C00.toInt(), // Orange
    0xFF5D4037.toInt(), // Brown
    0xFF455A64.toInt(), // Blue Grey
    0xFF212121.toInt(), // Dark Grey
)

@Composable
private fun ColorPicker(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cardColors) { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color.toUInt().toLong()), CircleShape)
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelected(color) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatDropdown(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Format") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BarcodeFormatMapper.displayNames.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format) },
                    onClick = {
                        onSelected(format)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EmbeddedScanner(
    onScanned: (value: String, format: String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.medium),
    ) {
        var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            onDispose {
                cameraProvider?.unbindAll()
                analysisExecutor.shutdown()
            }
        }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)

                future.addListener({
                    val provider = future.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                analysisExecutor,
                                BarcodeAnalyzer { barcodes ->
                                    barcodes.firstOrNull()?.let { barcode ->
                                        val value = barcode.rawValue ?: return@BarcodeAnalyzer
                                        val format = BarcodeFormatMapper.fromMlKit(barcode.format)
                                        onScanned(value, format)
                                    }
                                },
                            )
                        }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        Text(
            "Point camera at barcode",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}
