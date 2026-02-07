package com.example.devaudioreccordings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object RecordTranscription {

    private val _uploadDetails = MutableStateFlow(
        UploadDetails(
            isTranscriptionOnGoing = false,
            transcriptionState = TranscriptionState.LOADING,
            count = 0,
            flows = Flows.MediaCaptureService
        )
    )

    val uploadDetails: StateFlow<UploadDetails> = _uploadDetails

    fun updateDetails(areUploadsIncreasing: Boolean, transcriptionState: TranscriptionState) {
        CoroutineScope(Dispatchers.IO).launch {
            if (areUploadsIncreasing) {
                val data = UploadDetails(
                    isTranscriptionOnGoing = true,
                    transcriptionState,
                    (uploadDetails.value.count + 1),
                    Flows.MediaCaptureService
                )
                _uploadDetails.value = data
            } else {
                val data = UploadDetails(
                    isTranscriptionOnGoing = true,
                    transcriptionState,
                    uploadDetails.value.count ,
                    Flows.MediaCaptureService
                )

                _uploadDetails.value = data
                delay(1000)
                if ((uploadDetails.value.count - 1) == 0) {
                    val data2 = UploadDetails(
                        isTranscriptionOnGoing = false,
                        TranscriptionState.LOADING,
                        (uploadDetails.value.count - 1),
                        Flows.MediaCaptureService
                    )
                    _uploadDetails.value = data2
                } else {
                    val data2 = UploadDetails(
                        isTranscriptionOnGoing = true,
                        TranscriptionState.LOADING,
                        (uploadDetails.value.count - 1),
                        Flows.MediaCaptureService
                    )

                    _uploadDetails.value = data2
                    2
                }

            }
        }

    }
}

data class UploadDetails(
    val isTranscriptionOnGoing: Boolean,
    val transcriptionState: TranscriptionState,
    val count: Int,
    val flows: Flows
)

enum class TranscriptionState {
    SUCCESS, PARTIALSUCCESS, FAILURE, LOADING
}

// Dummy Flows class (create your real one)

