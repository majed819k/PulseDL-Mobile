package com.pulsedl.mobile.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream

object StorageHelper {
    private const val TAG = "StorageHelper"

    /**
     * Resolves a human-readable filesystem path from a Storage Access Framework (SAF) Tree URI
     * when the selected folder resides on primary shared storage or SD card.
     * If direct POSIX path resolution is restricted by Scoped Storage, returns null so the engine
     * can download to a fast staging folder and copy to the DocumentFile tree upon completion.
     */
    fun resolvePosixPathFromTreeUri(context: Context, treeUri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val split = docId.split(":")
            if (split.size >= 2) {
                val storageType = split[0]
                val relativePath = split[1]
                if ("primary".equals(storageType, ignoreCase = true)) {
                    val base = Environment.getExternalStorageDirectory().absolutePath
                    val target = File(base, relativePath)
                    if (target.exists() || target.mkdirs()) {
                        return target.absolutePath
                    }
                } else {
                    // Removable SD card volume (e.g. /storage/XXXX-XXXX/...)
                    val Candidate = File("/storage/$storageType", relativePath)
                    if (Candidate.exists() || Candidate.mkdirs()) {
                        return Candidate.absolutePath
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to resolve direct POSIX path from SAF URI: $treeUri", e)
            null
        }
    }

    /**
     * Returns a display-friendly path for UI badges from a SAF Tree URI.
     */
    fun getDisplayPathFromTreeUri(context: Context, treeUri: Uri): String {
        resolvePosixPathFromTreeUri(context, treeUri)?.let { return it }
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            "SAF://${docId.replace(":", "/")}"
        } catch (_: Exception) {
            treeUri.toString()
        }
    }

    /**
     * Ensures the default fallback directory inside public Downloads/PulseDL exists and is writable.
     */
    fun getDefaultPulseDlDirectory(subFolder: String = "PulseDL"): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(publicDownloads, subFolder)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies a completed media file into a SAF DocumentFile tree if the user picked a non-POSIX SAF URI,
     * and returns the final URI/path string.
     */
    fun copyFileToSafTreeIfNeeded(
        context: Context,
        sourceFile: File,
        safTreeUriString: String?
    ): String {
        if (safTreeUriString.isNullOrBlank()) {
            scanFileWithMediaScanner(context, sourceFile)
            return sourceFile.absolutePath
        }

        val treeUri = Uri.parse(safTreeUriString)
        val directPosix = resolvePosixPathFromTreeUri(context, treeUri)
        if (directPosix != null && sourceFile.absolutePath.startsWith(directPosix)) {
            // Already written directly in the target directory
            scanFileWithMediaScanner(context, sourceFile)
            return sourceFile.absolutePath
        }

        return try {
            val treeDir = DocumentFile.fromTreeUri(context, treeUri)
                ?: return sourceFile.absolutePath
            val mimeType = getMimeTypeForFile(sourceFile)
            val existing = treeDir.findFile(sourceFile.name)
            existing?.delete()

            val targetDoc = treeDir.createFile(mimeType, sourceFile.name)
                ?: return sourceFile.absolutePath

            context.contentResolver.openOutputStream(targetDoc.uri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream, bufferSize = 64 * 1024)
                }
            }
            sourceFile.delete()
            targetDoc.uri.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying to SAF DocumentFile tree, keeping local file", e)
            scanFileWithMediaScanner(context, sourceFile)
            sourceFile.absolutePath
        }
    }

    /**
     * Triggers Android's MediaScannerConnection so newly downloaded 4K/8K videos and FLAC/MP3 tracks
     * appear immediately inside Gallery, Google Photos, and Music players.
     */
    fun scanFileWithMediaScanner(context: Context, file: File) {
        if (!file.exists()) return
        val mimeType = getMimeTypeForFile(file)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType)
        ) { path, uri ->
            Log.d(TAG, "MediaScanner indexed file: $path -> $uri")
        }
    }

    fun getMimeTypeForFile(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (ext) {
                "mkv" -> "video/x-matroska"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mp3" -> "audio/mpeg"
                "flac" -> "audio/flac"
                "m4a" -> "audio/mp4"
                "opus" -> "audio/ogg"
                else -> "application/octet-stream"
            }
    }
}
