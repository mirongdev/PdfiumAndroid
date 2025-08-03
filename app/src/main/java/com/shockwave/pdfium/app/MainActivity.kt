package com.shockwave.pdfium.app

import android.graphics.Bitmap
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.app.ui.theme.PdfiumAndroidTheme
import java.io.IOException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SAMPLE_PDF_URL = "https://ontheline.trincoll.edu/images/bookdown/sample-local-pdf.pdf"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdfiumAndroidTheme {
                var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var showPdfViewer by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showPdfViewer) {
                        PdfViewerScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(onClick = { openPdf { bitmap -> pdfBitmap = bitmap } }) {
                                Text("Open PDF (Simple)")
                            }
                            
                            Button(onClick = { showPdfViewer = true }) {
                                Text("Open PDF (Full Viewer)")
                            }
                            
                            pdfBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "PDF Page",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PdfViewerScreen(modifier: Modifier = Modifier) {
        val scope = rememberCoroutineScope()
        var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }
        
        LaunchedEffect(Unit) {
            scope.launch {
                pdfBytes = withContext(Dispatchers.IO) {
                    try {
                        URL(SAMPLE_PDF_URL).readBytes()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading PDF", e)
                        null
                    }
                }
            }
        }
        
        AndroidView(
            modifier = modifier,
            factory = { context ->
                PDFView(context, null).apply {
                    // Initial setup if needed
                }
            },
            update = { pdfView ->
                pdfBytes?.let { bytes ->
                    pdfView.fromBytes(bytes)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .enableAnnotationRendering(true)
                        .enableAntialiasing(true)
                        .spacing(4)
                        .autoSpacing(true)
                        .pageFitPolicy(FitPolicy.WIDTH)
                        .pageSnap(true)
                        .pageFling(true)
                        .load()
                }
            }
        )
    }

    private fun openPdf(onBitmapReady: (Bitmap) -> Unit) {
        // TODO: Replace with actual PDF file descriptor
        val fd: ParcelFileDescriptor? = null
        val pageNum = 0
        val pdfiumCore = PdfiumCore(this)
        
        try {
            fd?.let { fileDescriptor ->
                val pdfDocument = pdfiumCore.newDocument(fileDescriptor)
                
                pdfiumCore.openPage(pdfDocument, pageNum)
                
                val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum)
                val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum)
                
                // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
                // RGB_565 - little worse quality, twice less memory usage
                val bitmap = Bitmap.createBitmap(
                    width, height,
                    Bitmap.Config.RGB_565
                )
                
                pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0, width, height)
                onBitmapReady(bitmap)
                
                printInfo(pdfiumCore, pdfDocument)
                
                pdfiumCore.closeDocument(pdfDocument) // important!
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun printInfo(core: PdfiumCore, doc: PdfDocument) {
        val meta = core.getDocumentMeta(doc)
        Log.e(TAG, "title = ${meta.title}")
        Log.e(TAG, "author = ${meta.author}")
        Log.e(TAG, "subject = ${meta.subject}")
        Log.e(TAG, "keywords = ${meta.keywords}")
        Log.e(TAG, "creator = ${meta.creator}")
        Log.e(TAG, "producer = ${meta.producer}")
        Log.e(TAG, "creationDate = ${meta.creationDate}")
        Log.e(TAG, "modDate = ${meta.modDate}")
        
        printBookmarksTree(core.getTableOfContents(doc), "-")
    }

    private fun printBookmarksTree(tree: List<PdfDocument.Bookmark>, sep: String) {
        for (b in tree) {
            Log.e(TAG, String.format("%s %s, p %d", sep, b.title, b.pageIdx))
            
            if (b.hasChildren()) {
                printBookmarksTree(b.children, "$sep-")
            }
        }
    }
}

