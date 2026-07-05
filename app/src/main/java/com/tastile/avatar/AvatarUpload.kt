package com.tastile.avatar

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable

/**
 * AvatarUpload — Avatar upload component (v1/15 §3).
 *
 * 3-step flow: pick file → resize to WebP → upload via presigned URL → commit.
 * Phase A: skeleton. Full implementation with BitmapFactory in Phase X.
 */
@Composable
fun AvatarUpload(
    onUploaded: (String) -> Unit
) {
    // TODO: Launch photo picker intent
    // TODO: BitmapFactory.decodeStream → scale to 256/64/32
    // TODO: WebP encode
    // TODO: POST /v1/uploads/avatar → presigned PUT
    // TODO: PUT to S3
    // TODO: POST /v1/uploads/avatar/{id}/commit
}

/**
 * Resize a bitmap to the specified dimension (square crop + scale).
 */
fun resizeBitmap(source: Bitmap, targetSize: Int): Bitmap {
    val size = minOf(source.width, source.height)
    val x = (source.width - size) / 2
    val y = (source.height - size) / 2
    val cropped = Bitmap.createBitmap(source, x, y, size, size)
    return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
}
