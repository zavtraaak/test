package com.notube.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/** Minimal Retrofit converter backed by kotlinx.serialization. */
class JsonConverterFactory(private val json: Json) : Converter.Factory() {
    private val mediaType: MediaType = "application/json; charset=UTF-8".toMediaType()

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        @Suppress("UNCHECKED_CAST")
        val serializer = json.serializersModule.serializer(type) as KSerializer<Any?>
        return Converter<ResponseBody, Any?> { body ->
            body.use { json.decodeFromString(serializer, it.string()) }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody> {
        @Suppress("UNCHECKED_CAST")
        val serializer = json.serializersModule.serializer(type) as KSerializer<Any?>
        return Converter<Any?, RequestBody> { value ->
            json.encodeToString(serializer, value).toRequestBody(mediaType)
        }
    }
}
