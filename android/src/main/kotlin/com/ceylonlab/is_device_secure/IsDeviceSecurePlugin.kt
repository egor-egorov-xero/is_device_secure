package com.ceylonlab.is_device_secure

import android.app.KeyguardManager
import android.content.Context
import android.os.Build

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


/** IsDeviceSecurePlugin */
public class IsDeviceSecurePlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context;

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = flutterPluginBinding.applicationContext;
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "is_device_secure")
        channel.setMethodCallHandler(this);
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "is_device_secure")
            channel.setMethodCallHandler(IsDeviceSecurePlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "isSecure") {
            result.success(isDeviceSecured());
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun isDeviceSecured(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager.isDeviceSecure
        } else {
            // keyguardManager.isKeyguardSecure "returns true if a PIN, pattern or password is set or a SIM card is locked."
            // We need to ignore the "SIM locked" bit, we can do this using the DevicePolicyManager
            // We also don't need to worry about Biometrics as support for these wasn't added until Marshmallow
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isKeyguardSecure) return false

            return when (Settings.Secure.getInt(contentResolver, PASSWORD_TYPE_KEY, DevicePolicyManager.PASSWORD_QUALITY_SOMETHING)) {
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING -> { // Pattern
                    Settings.Secure.getInt(contentResolver, Settings.Secure.LOCK_PATTERN_ENABLED, 0) == 1
                }
                DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC, // Password
                DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC, // Password
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC -> true // PIN

                else -> false
            }
        }
    }

    companion object {
        private const val PASSWORD_TYPE_KEY = "lockscreen.password_type"
    }
}
