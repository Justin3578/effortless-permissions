/*
 * Copyright © 2021 NINETalk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ninetalk.android.effortlesspermissions;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class EffortlessPermissions {

    private static final String TAG = EffortlessPermissions.class.getSimpleName();

    private EffortlessPermissions() {}

    public static boolean hasPermissions(Context context, @NonNull String... permissions) {
        return EasyPermissions.hasPermissions(context, permissions);
    }

    public static boolean hasPermissions(Fragment fragment, @NonNull String... permissions) {
        return EasyPermissions.hasPermissions(fragment.getContext(), permissions);
    }

    public static void requestPermissions(@NonNull Activity host, @NonNull String rationale,
                                          int requestCode, @NonNull String... permissions) {
        EasyPermissions.requestPermissions(host, rationale, requestCode, permissions);
    }

    public static void requestPermissions(@NonNull Fragment host, @NonNull String rationale,
                                          int requestCode, @NonNull String... permissions) {
        EasyPermissions.requestPermissions(host, rationale, requestCode, permissions);
    }

    public static void requestPermissions(PermissionRequest permissionRequest) {
        EasyPermissions.requestPermissions(permissionRequest);
    }

    public static void requestPermissions(@NonNull Activity host, @StringRes int rationale,
                                          int requestCode, @NonNull String... permissions) {
        requestPermissions(host, host.getString(rationale), requestCode, permissions);
    }

    public static void requestPermissions(@NonNull Fragment host, @StringRes int rationale,
                                          int requestCode, @NonNull String... permissions) {
        requestPermissions(host, host.getString(rationale), requestCode, permissions);
    }

    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                  @NonNull int[] grantResults,
                                                  @NonNull final Object... receivers) {
        Object[] allReceivers = new Object[receivers.length + 1];
        System.arraycopy(receivers, 0, allReceivers, 0, receivers.length);
        allReceivers[allReceivers.length - 1] = new EasyPermissions.PermissionCallbacks() {
            @Override
            public void onPermissionsGranted(int requestCode,
                                             List<String> grantedPermissions) {}
            @Override
            public void onPermissionsDenied(int requestCode,
                                            List<String> deniedPermissions) {
                for (Object object : receivers) {
                    runAfterPermissionDenied(object, requestCode, deniedPermissions);
                }
            }
            @Override
            public void onRequestPermissionsResult(int requestCode,
                                                   @NonNull String[] permissions,
                                                   @NonNull int[] grantResults) {}
        };
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,
                allReceivers);
    }

    public static boolean somePermissionPermanentlyDenied(@NonNull Activity host,
                                                          @NonNull List<String> permissions) {
        return EasyPermissions.somePermissionPermanentlyDenied(host, permissions);
    }

    public static boolean somePermissionPermanentlyDenied(@NonNull Fragment host,
                                                          @NonNull List<String> permissions) {
        return EasyPermissions.somePermissionPermanentlyDenied(host, permissions);
    }

    public static boolean somePermissionPermanentlyDenied(@NonNull Activity host,
                                                          @NonNull String... permissions) {
        return somePermissionPermanentlyDenied(host, Arrays.asList(permissions));
    }

    public static boolean somePermissionPermanentlyDenied(@NonNull Fragment host,
                                                          @NonNull String... permissions) {
        return EasyPermissions.somePermissionPermanentlyDenied(host, Arrays.asList(
                permissions));
    }

    public static boolean permissionPermanentlyDenied(@NonNull Activity host,
                                                      @NonNull String deniedPermission) {
        return EasyPermissions.permissionPermanentlyDenied(host, deniedPermission);
    }

    public static boolean permissionPermanentlyDenied(@NonNull Fragment host,
                                                      @NonNull String deniedPermission) {
        return EasyPermissions.permissionPermanentlyDenied(host, deniedPermission);
    }

    public static boolean somePermissionDenied(@NonNull Activity host,
                                               @NonNull String... permissions) {
        return EasyPermissions.somePermissionDenied(host, permissions);
    }

    public static boolean somePermissionDenied(@NonNull Fragment host,
                                               @NonNull String... permissions) {
        return EasyPermissions.somePermissionDenied(host, permissions);
    }

    /**
     * @see EasyPermissions#runAnnotatedMethods(Object, int)
     */
    private static void runAfterPermissionDenied(@NonNull Object object, int requestCode,
                                                 List<String> deniedPermissions) {
        Class clazz = object.getClass();
        if (isUsingAndroidAnnotations(object)) {
            clazz = clazz.getSuperclass();
        }
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AfterPermissionDenied.class)) {
                    AfterPermissionDenied annotation = method.getAnnotation(
                            AfterPermissionDenied.class);
                    if (annotation.value() == requestCode) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (!(parameterTypes.length == 0 || (parameterTypes.length == 1
                                && parameterTypes[0].isAssignableFrom(List.class)))) {
                            throw new RuntimeException("Cannot execute method " + method.getName() +
                                    " because its parameter list is not empty or containing only" +
                                    " a List<String>.");
                        }
                        try {
                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            if (parameterTypes.length == 0) {
                                method.invoke(object);
                            } else {
                                method.invoke(object, deniedPermissions);
                            }
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, "Running AfterPermissionDenied failed", e);
                        } catch (InvocationTargetException e) {
                            Log.e(TAG, "Running AfterPermissionDenied failed", e);
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * @see EasyPermissions#isUsingAndroidAnnotations(Object)
     */
    private static boolean isUsingAndroidAnnotations(@NonNull Object object) {
        if (!object.getClass().getSimpleName().endsWith("_")) {
            return false;
        }
        try {
            Class clazz = Class.forName("org.androidannotations.api.view.HasViews");
            return clazz.isInstance(object);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
