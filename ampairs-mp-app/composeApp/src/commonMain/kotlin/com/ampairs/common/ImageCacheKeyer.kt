package com.ampairs.common

import com.seiko.imageloader.component.keyer.Keyer
import com.seiko.imageloader.option.Options
import com.seiko.imageloader.option.Scale
import io.ktor.http.Url


val DEFAULT_MAX_IMAGE_SIZE = 4096

class ImageCacheKeyer : Keyer {

    private fun suffix(options: Options): String {
        return buildString {
            if (options.allowInexactSize) {
                append("-allowInexactSize")
            }
            if (!options.premultipliedAlpha) {
                append("-premultipliedAlpha")
            }
            if (options.scale != Scale.FILL) {
                append("-scale=fit")
            }
            if (!options.playAnimate) {
                append("-noPlay")
            }
            if (options.maxImageSize != DEFAULT_MAX_IMAGE_SIZE) {
                append("-maxSize${options.maxImageSize}")
            }
        }
    }

    override fun key(data: Any, options: Options): String? {
        if (data !is Url) return null
        val keySuffix = suffix(options)
        return data.host + data.encodedPath + keySuffix
    }
}