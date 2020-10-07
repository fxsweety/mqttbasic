package com.example.mqttbasic

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.regions.Regions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var awsMqttManager: AWSIotMqttManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val userPool = CognitoUserPool(
            applicationContext,
            "us-east-1_idOgwfCQZ", // add your user pool id
            "6dj41cc6hp85i0ip5lvbm0m56t", // your client id
            "",
            Regions.US_EAST_1 //region your endpoint is set up
        )
        var cognitoUser: CognitoUser = userPool.getUser("mqttuser") // set your cognito user


        var  token = ""

        // Callback handler for the sign-in process
        val authenticationHandler: AuthenticationHandler = object : AuthenticationHandler {

            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                token = userSession?.idToken?.jwtToken ?: "123"
                btnConnect.visibility = View.VISIBLE
            }

            override fun authenticationChallenge(continuation: ChallengeContinuation?) {

                Log.d("mqtt challenge", "")
            }

            override fun getAuthenticationDetails(
                authenticationContinuation: AuthenticationContinuation?,
                userId: String?
            ) {
                Log.d("mqtt authentication", "")
                val authenticationDetails = AuthenticationDetails("mqttuser", "Mqttuser@20", null)

                // Pass the user sign-in credentials to the continuation
                authenticationContinuation!!.setAuthenticationDetails(authenticationDetails)

                // Allow the sign-in to continue
                authenticationContinuation!!.continueTask()
            }

            override fun getMFACode(multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation) {
                Log.d("mqtt mfa", "")
            }

            override fun onFailure(exception: java.lang.Exception) {
                // Sign-in failed, check exception for the cause
                Log.d("mqtt sign-in failed", exception.toString())
            }
        }



        cognitoUser.getSessionInBackground(authenticationHandler)


        awsMqttManager =
            AWSIotMqttManager(
                "mqttClient", // the client Id specified in policy document
                "a1f9g6wyi4ucgp-ats.iot.us-east-1.amazonaws.com" // aws core -> settings
            )


        // Connect and Subscribe button
        btnConnect.setOnClickListener {
            val login: MutableMap<String, String> = HashMap()
            Log.d("mqtt Token id is ", token)
            // login[cognito-idp.<region>.amazonaws.com/<user-pool-id>"] = idToken
            login["cognito-idp.us-east-1.amazonaws.com/us-east-1_idOgwfCQZ"] = token
            val credentialsProvider = CognitoCachingCredentialsProvider(
                applicationContext,
                "us-east-1:857b072e-312b-46b6-a063-10e26d5afe29",  // Identity pool ID
                Regions.US_EAST_1 // Region
            )
            credentialsProvider.logins = login

            awsMqttManager.connect(credentialsProvider, statusCallback)
        }


        btnPublish.setOnClickListener {
            awsMqttManager.publishString((publishMsg as EditText).text.toString(), "mqttexample", AWSIotMqttQos.QOS0)
        }

        btnDisconnect.setOnClickListener {
            awsMqttManager.unsubscribeTopic("mqttexample")
            awsMqttManager.disconnect()
        }
    }


     var statusCallback: AWSIotMqttClientStatusCallback =
        AWSIotMqttClientStatusCallback { status, throwable ->

            when (status) {
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connecting -> {
                    Log.i("MQTT onStatusChanged",  "- Connecting")
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected -> {
                    Log.i("MQTT onStatusChanged -", "Connected")
                    subscribeToTopic()
                }
                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Reconnecting -> {
                    Log.i("MQTT onStatusChanged -", " Reconnecting")
                }

                AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.ConnectionLost -> {
                    Log.i("MQTT onStatusChanged -", "Connection Lost")
                }
            }
        }

        private fun subscribeToTopic() {
            awsMqttManager.subscribeToTopic("mqttexample", AWSIotMqttQos.QOS0,  AWSIotMqttNewMessageCallback { callbackTopic, data ->
                run {
                    Log.i("mqtt callback topic", callbackTopic)
                    Log.i("mqtt callback message", String(data, Charsets.UTF_8))
                    message.text = String(data, Charsets.UTF_8)
                }
            })

    }






}

