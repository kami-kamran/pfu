package com.lib.pay.from.libpfu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.lib.pay.from.libpfu.callbacks.PaymentCallbacks
import com.lib.pay.from.libpfu.di.NetworkModule
import com.lib.pay.from.libpfu.models.CreatePaymentRequestModel
import com.lib.pay.from.libpfu.repo.PaymentRepository
import kotlinx.coroutines.launch
import javax.inject.Inject


class PaymentManagerImp @Inject constructor(
    private val paymentRepository: PaymentRepository
) : PaymentManager {
    private var paymentCallbacks: PaymentCallbacks? = null
    private var resultLauncher: ActivityResultLauncher<Intent>? = null

    // Initialize with Bearer Token
    override fun initialize(activity: ComponentActivity) {
        activity.setResultLauncher()
    }

    // Initialize result launcher inside the Activity or Fragment
    private fun ComponentActivity.setResultLauncher() {
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val response = data?.getStringExtra("response")
                    //Toast.makeText(this, "RESULT_OK: response ${data?.getStringExtra("response")?:"null"}", Toast.LENGTH_SHORT).show()
                    Log.d("PaymentManager", "response: $response")
                    if (response != null) {
                        paymentCallbacks?.onPaymentSuccess(response)
                        lifecycleScope.launch {
                            submitPaymentStatus(title = response, description = response)
                        }
                    } else {
                        paymentCallbacks?.onPaymentFailed("Payment not completed Response is null")
                    }
                } else {
                    paymentCallbacks?.onPaymentFailed("Payment not completed")
                }
            }
    }

    // Set callback listeners
    override fun setCallbacks(callbacks: PaymentCallbacks) {
        this.paymentCallbacks = callbacks
    }

    // Example API call function for creating a transaction
    override suspend fun createTransaction(
        context: Context,
        bearerToken: String,
        userName: String,
        email: String,
        mobile: String,
        amount: Int,
        redirectUrl: String
    ) {
        // Set the Bearer token in the Network module
        NetworkModule.setBearerToken(context, bearerToken)
        // Call the repository to make the API call
        paymentRepository.createTransaction(
            requestModel = CreatePaymentRequestModel(
                from = "SDK_DIRECT_INTENT",
                type = "any",
                userName = userName,
                userEmail = email,
                userMobile = mobile,
                amount = amount,
                redirectUrl = redirectUrl
            ),
            onCreateSuccess = { response ->
                paymentCallbacks?.onCreateSuccess(response)
                openQueryUrl(context = context, url = response.data?.queryUrl)
            },
            onCreateFailed = { error ->
                paymentCallbacks?.onCreateFailed(error)
            }
        )
    }

    // Example of another API call for submitting payment status
    private suspend fun submitPaymentStatus(title: String, description: String) {
        paymentRepository.submitPaymentStatus(
            title = title,
            description = description,
            onPaymentSubmitSuccess = { response ->
                paymentCallbacks?.onPaymentSubmitSuccess(response)
            },
            onPaymentSubmitFailed = { error ->
                paymentCallbacks?.onPaymentSubmitFailed(error)
            }
        )
    }

    private fun openQueryUrl(context: Context, url: String?) {
       // Toast.makeText(context, "QueryUrl: ${url?:"null"}", Toast.LENGTH_SHORT).show()
        if (!url.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(context.packageManager) != null) {
                resultLauncher?.launch(intent)
            } else {
                paymentCallbacks?.onPaymentFailed("No application available to handle this request!");
            }
        } else {
            paymentCallbacks?.onPaymentFailed("Invalid payment query url");
        }

    }
}

