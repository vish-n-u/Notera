package com.example.devaudioreccordings

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {
    /**
     * Check for permissions, request them if they're not granted.
     *
     * @return true if permissions are already granted, else request them and return false.
     */
    fun checkAndRequestPermissions(
        activity: Activity?,
        requestCode: Int,
        permissionList: Array<String>
    ): Boolean {
        val toRequest: MutableList<String> = ArrayList()
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                toRequest.add(permission)
            }
        }
        if (toRequest.size > 0) {
            val requestedPermissions = toRequest.toTypedArray<String>()
            ActivityCompat.requestPermissions(activity, requestedPermissions, requestCode)
            return false
        }
        return true
    }

    /**
     * Check for permissions, request them if they're not granted.
     *
     * @return true if permissions are already granted, else request them and return false.
     */
    private fun checkAndRequestPermissions(
        fragment: Fragment,
        requestCode: Int,
        permissionList: Array<String>
    ): Boolean {
        val toRequest: MutableList<String> = ArrayList()
        for (permission in permissionList) {
            val context: Context? = fragment.activity
            if (context != null && ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager
                    .PERMISSION_GRANTED
            ) {
                toRequest.add(permission)
            }
        }
        if (toRequest.size > 0) {
            val requestedPermissions = toRequest.toTypedArray<String>()
            fragment.requestPermissions(requestedPermissions, requestCode)
            return false
        }
        return true
    }

    /**
     * Check for permissions without requesting them
     *
     * @return true if all permissions are granted
     */
    fun checkPermissions(activity: Activity?, permissionList: Array<String?>): Boolean {
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun checkCameraAndStoragePermissions(activity: Activity?): Boolean {
        return checkPermissions(
            activity,
            arrayOf(
                permission.WRITE_EXTERNAL_STORAGE,
                permission.CAMERA
            )
        )
    }

    fun checkAndRequestCameraAndStoragePermissions(fragment: Fragment, requestCode: Int): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(permission.CAMERA)
        } else {
            arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
        }
        return checkAndRequestPermissions(fragment, requestCode, permissions)
    }

    fun checkAndRequestCameraAndStoragePermissions(activity: Activity?, requestCode: Int): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(permission.CAMERA)
        } else {
            arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
        }
        return checkAndRequestPermissions(activity, requestCode, permissions)
    }

    fun checkAndRequestStoragePermission(activity: Activity?, requestCode: Int): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(permission.CAMERA)
        } else {
            arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
        }
        return checkAndRequestPermissions(activity, requestCode, permissions)
    }

    fun checkAndRequestStoragePermission(fragment: Fragment, requestCode: Int): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(permission.CAMERA)
        } else {
            arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
        }
        return checkAndRequestPermissions(fragment, requestCode, permissions)
    }
}
