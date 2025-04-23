package com.lib.pay.from.libpfu.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SubmitPaymentResponse(
    @SerializedName("success")
    @Expose
    val success: Boolean = false,
    @SerializedName("paymentApproved")
    @Expose
    val paymentApproved: Boolean = false,
    @SerializedName("message")
    @Expose
    val message: String,
)


