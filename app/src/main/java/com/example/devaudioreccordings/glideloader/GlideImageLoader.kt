package com.example.devaudioreccordings.glideloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import org.wordpress.aztec.Html
import org.wordpress.aztec.glideloader.extensions.upscaleTo

class GlideImageLoader(private val context: Context) : Html.ImageGetter {

    override fun loadImage(source: String, callbacks: Html.ImageGetter.Callbacks, maxWidth: Int) {
        loadImage(source, callbacks, maxWidth, 0)
    }

    override fun loadImage(source: String, callbacks: Html.ImageGetter.Callbacks, maxWidth: Int, minWidth: Int) {
        if (source.startsWith("drawable://")) {
            val drawableName = source.removePrefix("drawable://")
            val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)

            if (resId != 0) {
                val drawable = ContextCompat.getDrawable(context, resId)
                callbacks.onImageLoaded(drawable)
            } else {
                callbacks.onImageFailed()
            }

            return
        }

        // Else fallback: load with Glide normally (for file://, http://, content://, etc.)
        Glide.with(context)
            .asBitmap()
            .load(source)
            .into(object : Target<Bitmap> {
                override fun onLoadStarted(placeholder: Drawable?) {
                    callbacks.onImageLoading(placeholder)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    callbacks.onImageFailed()
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (resource.width < minWidth) {
                        return callbacks.onImageLoaded(BitmapDrawable(context.resources, resource.upscaleTo(minWidth)))
                    }
                    resource.density = DisplayMetrics.DENSITY_DEFAULT
                    callbacks.onImageLoaded(BitmapDrawable(context.resources, resource))
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun getSize(cb: SizeReadyCallback) { cb.onSizeReady(maxWidth, Target.SIZE_ORIGINAL) }
                override fun removeCallback(cb: SizeReadyCallback) {}
                override fun setRequest(request: Request?) {}
                override fun getRequest(): Request? = null
                override fun onStart() {}
                override fun onStop() {}
                override fun onDestroy() {}
            })
    }
}
