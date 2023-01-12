package com.umair.beacons_plugin

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import timber.log.Timber


/** BeaconsPlugin */
class BeaconsPlugin : FlutterPlugin, ActivityAware {

    private var context: Context? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Timber.i("onAttachedToEngine")
        messenger = flutterPluginBinding.binaryMessenger
        setUpPluginMethods(
            flutterPluginBinding.binaryMessenger
        )
        context = flutterPluginBinding.applicationContext
        beaconHelper = BeaconHelper(flutterPluginBinding.applicationContext)
        context?.let {
            BeaconPreferences.init(it)
        }
    }

    companion object {
        private val TAG = "BeaconsPlugin"
        private var channel: MethodChannel? = null
        private var event_channel: EventChannel? = null
        private var currentActivity: Activity? = null
        private var beaconHelper: BeaconHelper? = null

        @JvmStatic
        internal var messenger: BinaryMessenger? = null

        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            BeaconPreferences.init(registrar.context())
            if (beaconHelper == null) {
                this.beaconHelper = BeaconHelper(registrar.context())
            }
            registrar.activity()?.let { setUpPluginMethods(registrar.messenger()) }
        }

        @JvmStatic
        fun registerWith(messenger: BinaryMessenger, context: Context) {
            BeaconPreferences.init(context)
            if (beaconHelper == null) {
                this.beaconHelper = BeaconHelper(context)
            }
            setUpPluginMethods(messenger)
        }

        @JvmStatic
        fun registerWith(messenger: BinaryMessenger, beaconHelper: BeaconHelper, context: Context) {
            BeaconPreferences.init(context)
            this.beaconHelper = beaconHelper
            setUpPluginMethods(messenger)
        }

        @JvmStatic
        private fun setUpPluginMethods(messenger: BinaryMessenger) {
            Timber.plant(Timber.DebugTree())
            this.callBack = beaconHelper

            channel = MethodChannel(messenger, "beacons_plugin")
            channel?.setMethodCallHandler { call, result ->
                when {
                    call.method == "startMonitoring" -> {
                        callBack?.startScanning()
                        result.success("Started scanning Beacons.")
                    }
                    call.method == "stopMonitoring" -> {

                        callBack?.stopMonitoringBeacons()
                        result.success("Stopped scanning Beacons.")
                    }
                    call.method == "addRegion" -> {
                        callBack?.addRegion(call, result)
                    }
                    call.method == "clearRegions" -> {
                        callBack?.clearRegions(call, result)
                    }
                    call.method == "addBeaconLayoutForAndroid" -> {
                        call.argument<String>("layout")?.let {
                            callBack?.addBeaconLayout(it)
                            result.success("Beacon layout added: $it")
                        }
                    }
                    call.method == "setForegroundScanPeriodForAndroid" -> {
                        var foregroundScanPeriod = 1100L
                        var foregroundBetweenScanPeriod = 0L
                        call.argument<Int>("foregroundScanPeriod")?.let {
                            if (it > foregroundScanPeriod) {
                                foregroundScanPeriod = it.toLong()
                            }
                        }
                        call.argument<Int>("foregroundBetweenScanPeriod")?.let {
                            if (it > foregroundBetweenScanPeriod) {
                                foregroundBetweenScanPeriod = it.toLong()
                            }
                        }
                        callBack?.setForegroundScanPeriod(
                            foregroundScanPeriod = foregroundScanPeriod,
                            foregroundBetweenScanPeriod = foregroundBetweenScanPeriod
                        )
                        result.success("setForegroundScanPeriod updated.")
                    }
                    else -> result.notImplemented()
                }
            }

            event_channel = EventChannel(messenger, "beacons_plugin_stream")
            event_channel?.setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    callBack?.setEventSink(events)
                }

                override fun onCancel(arguments: Any?) {

                }
            })
        }


        interface PluginImpl {
            fun startScanning()
            fun stopMonitoringBeacons()
            fun addRegion(call: MethodCall, result: MethodChannel.Result)
            fun clearRegions(call: MethodCall, result: MethodChannel.Result)
            fun setEventSink(events: EventChannel.EventSink?)
            fun addBeaconLayout(layout: String)
            fun setForegroundScanPeriod(
                foregroundScanPeriod: Long,
                foregroundBetweenScanPeriod: Long
            )
        }

        private var callBack: PluginImpl? = null

    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        currentActivity = null
        channel?.setMethodCallHandler(null)
        event_channel?.setStreamHandler(null)

        beaconHelper?.stopMonitoringBeacons()

        context = null
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        currentActivity = activityPluginBinding.activity
        BeaconPreferences.init(currentActivity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Timber.i("onDetachedFromActivityForConfigChanges")
    }

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        Timber.i("onReattachedToActivityForConfigChanges")
        currentActivity = activityPluginBinding.activity
    }

    override fun onDetachedFromActivity() {
        Timber.i("onDetachedFromActivity")
        currentActivity = null
    }
}
