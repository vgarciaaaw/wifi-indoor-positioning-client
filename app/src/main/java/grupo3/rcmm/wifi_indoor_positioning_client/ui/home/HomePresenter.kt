package grupo3.rcmm.wifi_indoor_positioning_client.ui.home

import android.Manifest
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import grupo3.rcmm.wifi_indoor_positioning_client.R
import grupo3.rcmm.wifi_indoor_positioning_client.data.base.DataManager
import grupo3.rcmm.wifi_indoor_positioning_client.data.home.repository.HomeDataManager
import grupo3.rcmm.wifi_indoor_positioning_client.data.home.model.Waypoint
import grupo3.rcmm.wifi_indoor_positioning_client.ui.base.BasePresenter
import grupo3.rcmm.wifi_indoor_positioning_client.ui.base.IPresenter

/**
 * Created by victor on 28/04/18.
 */
class HomePresenter<V : HomeView> : BasePresenter<V>, IPresenter<V> {

    companion object {
        private val TAG: String = "HomePresenter"
    }

    private var menuId: Int = R.id.positioning

    constructor(dataManager: DataManager) : super(dataManager)

    override fun onAttach(view: V) {
        super.onAttach(view)
        getView().loadNavigationDrawer()
        getView().loadActionBar()
        getView().loadGoogleMap()
    }

    fun onNavigationItemSelected(itemId: Int) {
        menuId = itemId
        when (itemId) {
            R.id.positioning -> getView().showPositioning()
            R.id.waypoints -> getView().showWaypoints()
            R.id.fingerprinting -> getView().showFingerprinting()
        }
        getView().closeDrawerLayout()
    }

    fun onMenuOptionSelected(menuOptionId: Int) {
        when (menuOptionId) {
            android.R.id.home -> getView().openDrawerLayout()
        }
    }

    fun onMapReady() {
        getView().setMapListeners()
        getView().drawFloorPlan()
        (getDataManager() as HomeDataManager).getWaypoints()
                .observe(getView() as LifecycleOwner, Observer {
                    Log.d(TAG, "fetching " + it?.size.toString() + " waypoints")
                    getView().drawWaypoints(it!!)
                })
    }

    fun onMapLongClick(position: LatLng) {
        if (menuId == R.id.waypoints)
            (getDataManager() as HomeDataManager)
                    .addWaypoint(Waypoint(position.latitude, position.longitude))
                    .observe(getView() as LifecycleOwner, Observer {
                        getView().addMarker(it.toString(), position)
                    })

    }

    fun onMarkerClick(marker: Marker) {
        if (menuId == R.id.fingerprinting)
            Dexter.withActivity(getView() as AppCompatActivity)
                    .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    .withListener(object : PermissionListener {
                        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                            //This permission doesn't need rationale
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) =
                                getView().showToast(R.string.permission_denied)

                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            (getDataManager() as HomeDataManager)
                                    .getAccessPointMeasurements()
                                    .observe(getView() as LifecycleOwner,
                                            Observer {
                                                Log.d(TAG, "scanned " + it?.size + " access points...")
                                                //TODO send data to api
                                            })
                        }
                    }).check()
    }

    fun onMarkerDrag(position: Point, deleteButton: View) {
        if (menuId == R.id.waypoints) {
            if (overlaps(position, deleteButton))
                getView().activateDeleteButton()
            else
                getView().deactivateDeleteButton()
        }
    }

    fun onMarkerDragEnd(projectedPosition: Point, deleteButton: View,
                        marker: Marker, markerPosition: LatLng) {
        if (menuId == R.id.waypoints) {
            getView().hideDeleteButton()
            getView().deactivateDeleteButton()
            if (overlaps(projectedPosition, deleteButton)) {
                getView().deleteMarker(marker)
                (getDataManager() as HomeDataManager).deleteWaypoint(marker.title.toLong())
            } else
                (getDataManager() as HomeDataManager).updateWaypoint(Waypoint(marker.title.toLong(),
                        marker.position.latitude, marker.position.longitude))
        }
    }

    fun onMarkerDragStart() {
        if (menuId == R.id.waypoints)
            getView().showDeleteButton()
    }

    private fun overlaps(point: Point, view: View): Boolean {
        var viewPosition = IntArray(2)
        view.getLocationOnScreen(viewPosition)
        val overlapX: Boolean = point.x < viewPosition[0] + view.getWidth() && point.x > viewPosition[0] - view.getWidth()
        val overlapY: Boolean = point.y < viewPosition[1] + view.getHeight() && point.y > viewPosition[1] - view.getWidth()
        return overlapX && overlapY
    }
}