package com.mueblesjamar.whatsappshare

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.io.File

private const val PACKAGE_WHATSAPP          = "com.whatsapp"
private const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

@CapacitorPlugin(name = "WhatsAppShare")
class WhatsAppSharePlugin : Plugin() {

    /**
     * Open a specific WhatsApp chat identified by JID and pre-fill a message.
     *
     * Options:
     *   jid     – "5491112345678@s.whatsapp.net" or "5491112345678"
     *   message – text to pre-fill
     *   app     – "whatsapp" (default) | "whatsapp_business"
     */
    @PluginMethod
    fun shareToContact(call: PluginCall) {
        val jid     = call.getString("jid")     ?: return call.reject("jid is required")
        val message = call.getString("message") ?: return call.reject("message is required")
        val pkg     = packageForKey(call.getString("app") ?: "whatsapp")
        val phone   = extractPhone(jid)

        if (!isPackageInstalled(pkg)) {
            return call.reject("$pkg is not installed on this device")
        }

        try {
            val uri = Uri.parse("https://api.whatsapp.com/send").buildUpon()
                .appendQueryParameter("phone", phone)
                .appendQueryParameter("text", message)
                .build()

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            activity.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to open WhatsApp: ${e.message}", e)
        }
    }

    /**
     * Share one or more files (path or content:// URI) to WhatsApp.
     *
     * Options:
     *   filePaths – JSON array of paths or content:// URIs
     *   mimeType  – MIME type override; auto-detected if omitted
     *   message   – optional caption
     *   app       – "whatsapp" (default) | "whatsapp_business"
     */
    @PluginMethod
    fun shareFile(call: PluginCall) {
        val pathsArray = call.getArray("filePaths")
            ?: return call.reject("filePaths is required")

        val mimeType = call.getString("mimeType")
        val message  = call.getString("message")
        val pkg      = packageForKey(call.getString("app") ?: "whatsapp")

        if (!isPackageInstalled(pkg)) {
            return call.reject("$pkg is not installed on this device")
        }

        try {
            val uris = resolvePathsToUris(pathsArray)
            if (uris.isEmpty()) return call.reject("No valid files found")

            val resolvedMime = mimeType ?: commonMimeType(pathsArray)
            buildAndSendFileIntent(uris, resolvedMime, message, pkg)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to share file: ${e.message}", e)
        }
    }

    /**
     * Decode Base64 strings to temp files and share them to WhatsApp.
     *
     * Options:
     *   files   – JSON array of { base64, fileName, mimeType? }
     *   message – optional caption
     *   app     – "whatsapp" (default) | "whatsapp_business"
     */
    @PluginMethod
    fun shareFileFromBase64(call: PluginCall) {
        val filesArray = call.getArray("files")
            ?: return call.reject("files is required")

        val message = call.getString("message")
        val pkg     = packageForKey(call.getString("app") ?: "whatsapp")

        if (!isPackageInstalled(pkg)) {
            return call.reject("$pkg is not installed on this device")
        }

        try {
            val uris      = ArrayList<Uri>()
            val mimeTypes = mutableSetOf<String>()

            for (i in 0 until filesArray.length()) {
                val item     = filesArray.getJSONObject(i)
                val base64   = item.getString("base64")
                val fileName = item.getString("fileName")
                val mime     = if (item.has("mimeType")) item.getString("mimeType") else "*/*"

                val bytes    = Base64.decode(base64, Base64.DEFAULT)
                val tempFile = File(context.cacheDir, fileName)
                tempFile.writeBytes(bytes)

                val authority  = "${context.packageName}.whatsappshare.fileprovider"
                val contentUri = FileProvider.getUriForFile(context, authority, tempFile)
                uris.add(contentUri)
                mimeTypes.add(mime)
            }

            if (uris.isEmpty()) return call.reject("No files to share")

            val resolvedMime = if (mimeTypes.size == 1) mimeTypes.first() else "*/*"
            buildAndSendFileIntent(uris, resolvedMime, message, pkg)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to share file from base64: ${e.message}", e)
        }
    }

    /** Returns whether the requested WhatsApp variant is installed. */
    @PluginMethod
    fun isInstalled(call: PluginCall) {
        val pkg    = packageForKey(call.getString("app") ?: "whatsapp")
        val result = JSObject()
        result.put("value", isPackageInstalled(pkg))
        call.resolve(result)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildAndSendFileIntent(
        uris: ArrayList<Uri>,
        mimeType: String,
        message: String?,
        pkg: String
    ) {
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }

        intent.apply {
            if (!message.isNullOrEmpty()) putExtra(Intent.EXTRA_TEXT, message)
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(intent)
    }

    private fun resolvePathsToUris(pathsArray: JSArray): ArrayList<Uri> {
        val uris      = ArrayList<Uri>()
        val authority = "${context.packageName}.whatsappshare.fileprovider"

        for (i in 0 until pathsArray.length()) {
            val path = pathsArray.getString(i) ?: continue

            val uri: Uri? = when {
                path.startsWith("content://") -> {
                    // content:// URIs from the system picker have temporary permissions
                    // bound to the originating Intent. Copy to cacheDir so we can share
                    // via our own FileProvider (which WhatsApp can access).
                    val tempFile = copyContentUriToCache(Uri.parse(path)) ?: continue
                    FileProvider.getUriForFile(context, authority, tempFile)
                }
                path.startsWith("file://") -> {
                    val file = File(Uri.parse(path).path ?: continue)
                    if (!file.exists()) continue
                    FileProvider.getUriForFile(context, authority, file)
                }
                else -> {
                    val file = File(path)
                    if (!file.exists()) continue
                    FileProvider.getUriForFile(context, authority, file)
                }
            }

            if (uri != null) uris.add(uri)
        }

        return uris
    }

    /**
     * Copies a content:// URI into the app's cacheDir and returns the temp File,
     * or null if the copy fails. The original file name is preserved when possible.
     */
    private fun copyContentUriToCache(contentUri: Uri): File? {
        return try {
            val resolver = context.contentResolver

            // Try to get the original display name
            var fileName = "wa_share_${System.currentTimeMillis()}"
            resolver.query(contentUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (col >= 0) fileName = cursor.getString(col) ?: fileName
                }
            }

            val tempFile = File(context.cacheDir, fileName)
            resolver.openInputStream(contentUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun commonMimeType(pathsArray: JSArray): String {
        val types = mutableSetOf<String>()
        for (i in 0 until pathsArray.length()) {
            val path = pathsArray.getString(i) ?: continue
            val mime = if (path.startsWith("content://")) {
                // Ask ContentResolver for the real MIME type
                context.contentResolver.getType(Uri.parse(path)) ?: "*/*"
            } else {
                extensionToMime(path.substringAfterLast('.', "").lowercase())
            }
            types.add(mime)
        }
        return if (types.size == 1) types.first() else "*/*"
    }

    private fun extensionToMime(ext: String): String = when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "webp"        -> "image/webp"
        "mp4"         -> "video/mp4"
        "mov"         -> "video/quicktime"
        "mp3"         -> "audio/mpeg"
        "pdf"         -> "application/pdf"
        "doc"         -> "application/msword"
        "docx"        -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls"         -> "application/vnd.ms-excel"
        "xlsx"        -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else          -> "*/*"
    }

    private fun packageForKey(key: String): String =
        if (key == "whatsapp_business") PACKAGE_WHATSAPP_BUSINESS else PACKAGE_WHATSAPP

    private fun extractPhone(jid: String): String =
        if (jid.contains('@')) jid.substringBefore('@') else jid

    private fun isPackageInstalled(pkg: String): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
