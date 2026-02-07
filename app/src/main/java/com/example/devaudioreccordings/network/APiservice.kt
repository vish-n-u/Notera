package com.example.devaudioreccordings.network

import com.google.errorprone.annotations.Keep
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

@Keep
data class TextRequest(
    @SerializedName("uid")
    val uid: String,
    @SerializedName("text")
    val text: String
)

@Keep
data class IdRequest(
    @SerializedName("id")
    val id: String
)

@Keep
data class EnhanceTextResponse(
    @SerializedName("response")
    val response: String,
    @SerializedName("usedEnhanceTextCount")
    val usedEnhanceTextCount: Int,
    @SerializedName("totalEnhanceTextCount")
    val totalEnhanceTextCount: Int
)

@Keep
data class LimitIncreaseResponse(
    @SerializedName("response")
    val response: String
    // NOTE: This was incomplete — add other fields if needed
)

@Keep
data class LinkedinTextResponse(
    @SerializedName("response")
    val response: String,
    @SerializedName("linkedinTextConversionCount")
    val linkedinTextConversionCount: Int,
    @SerializedName("totalLinkedinTextConversionCount")
    val totalLinkedinTextConversionCount: Int
)

@Keep
data class UploadFileResonse(
    @SerializedName("response")
    val response: String,
    @SerializedName("Used_Transcription_Duration")
    val Used_Transcription_Duration: String,
    @SerializedName("Total_Transcription_Duration")
    val Total_Transcription_Duration: String
)

@Keep
data class CreateUserResponse(
    @SerializedName("message")
    val message: String
)


interface ApiService {
    @Multipart
    @POST("uploadFile")
    suspend fun uploadFile(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Part file: MultipartBody.Part,
        @Part uid: MultipartBody.Part,
        @Part saveContentAs: MultipartBody.Part,
        @Part timestamps : MultipartBody.Part
    ): UploadFileResonse

    @POST("user")
    suspend fun createUser(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Body id: IdRequest
    ): Response<CreateUserResponse>

    @POST("linkedinShareableText")
    suspend fun createLinkedinShareableText(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Body request: TextRequest
    ): LinkedinTextResponse

    @POST("enhanceText")
    suspend fun enhanceText(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Body request: TextRequest
    ):EnhanceTextResponse

    @POST("increase")
    suspend fun increaseLimit(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Body request: TextRequest
    ):String
}


object RetrofitInstance {
    val baseUrl = "https://audiotext-backend-tvik.onrender.com/"

//        val baseUrl = "http://10.0.2.2:3000/"
//        val baseUrl = "http://192.168.1.4:3000/"
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY // Shows full request & response
}
    val client = OkHttpClient.Builder()
        .readTimeout(190, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(200, TimeUnit.SECONDS)     // Increase read timeout
        .writeTimeout(190, TimeUnit.SECONDS)    // Increase write timeout
        .build()

    val retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(
        baseUrl
    ).client(client).build()
    val ApiServices = retrofit.create(ApiService::class.java)

}