package grupo3.rcmm.wifi_indoor_positioning_client.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import grupo3.rcmm.wifi_indoor_positioning_client.data.model.AccessPointMeasurement
import grupo3.rcmm.wifi_indoor_positioning_client.data.event.AccessPointsEvent
import grupo3.rcmm.wifi_indoor_positioning_client.R
import grupo3.rcmm.wifi_indoor_positioning_client.data.service.WifiService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.MapFragment




class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private val TAG: String = "Home Activity"

    private val REQUEST_PERMISSION_CODE: Int = 1

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        loadGoogleMap()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            startListeningWifi()
        else {
            val accessCoarseLocationPermission = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, accessCoarseLocationPermission, REQUEST_PERMISSION_CODE)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListeningWifi()
    }

    private fun startListeningWifi() {
        val intent = Intent(this, WifiService::class.java)
        startService(intent)
    }

    private fun stopListeningWifi() {
        val intent = Intent(this, WifiService::class.java)
        stopService(intent)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onAccessPointsMeasured(apMeasurements: AccessPointsEvent) {
        val formattedMeasurements = mutableListOf<AccessPointMeasurement>()
        for (accesPoint in apMeasurements.accessPoints) {
            formattedMeasurements.add(AccessPointMeasurement(accesPoint.BSSID, accesPoint.level))
        }
        Log.d(TAG, formattedMeasurements.toString())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startListeningWifi()
                else
                    Toast.makeText(this, "Necesitas conceder permisos para utilizar la app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGoogleMap() {
        val mapFragment = fragmentManager
                .findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap?) {
        if (map != null) {
            map.addMarker(MarkerOptions()
                    .position(LatLng(39.478896, -6.34246)))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.478896, -6.34246), 100f))
        }
    }
}