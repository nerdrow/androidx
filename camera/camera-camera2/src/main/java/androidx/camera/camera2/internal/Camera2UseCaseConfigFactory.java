/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.internal;

import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER;

import android.content.Context;
import android.graphics.Point;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.workaround.PreviewPixelHDRnet;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;

/**
 * Implementation of UseCaseConfigFactory to provide the default camera2 configurations for use
 * cases.
 */
public final class Camera2UseCaseConfigFactory implements UseCaseConfigFactory {
    private static final Size MAX_PREVIEW_SIZE = new Size(1920, 1080);
    final WindowManager mWindowManager;

    public Camera2UseCaseConfigFactory(@NonNull Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Returns the configuration for the given capture type, or <code>null</code> if the
     * configuration cannot be produced.
     */
    @Nullable
    @Override
    @SuppressWarnings("deprecation") /* getDefaultDisplay */
    public Config getConfig(@NonNull CaptureType captureType) {
        final MutableOptionsBundle mutableConfig = MutableOptionsBundle.create();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        // TODO(b/114762170): Must set to preview here until we allow for multiple template
        //  types
        sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        if (captureType == CaptureType.PREVIEW) {
            // Set the WYSIWYG preview for CAPTURE_TYPE_PREVIEW
            PreviewPixelHDRnet.setHDRnet(sessionBuilder);
        }

        mutableConfig.insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionBuilder.build());

        mutableConfig.insertOption(OPTION_SESSION_CONFIG_UNPACKER,
                Camera2SessionOptionUnpacker.INSTANCE);

        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();

        // Only CAPTURE_TYPE_IMAGE_CAPTURE uses CameraDevice.TEMPLATE_STILL_CAPTURE. Other
        // capture types all use CameraDevice.TEMPLATE_PREVIEW.
        switch (captureType) {
            case IMAGE_CAPTURE:
                captureBuilder.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);
                break;
            case PREVIEW:
            case IMAGE_ANALYSIS:
            case VIDEO_CAPTURE:
                captureBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
                break;
        }
        mutableConfig.insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureBuilder.build());

        // Only CAPTURE_TYPE_IMAGE_CAPTURE has its own ImageCaptureOptionUnpacker. Other
        // capture types all use the standard Camera2CaptureOptionUnpacker.
        mutableConfig.insertOption(OPTION_CAPTURE_CONFIG_UNPACKER,
                captureType == CaptureType.IMAGE_CAPTURE ? ImageCaptureOptionUnpacker.INSTANCE
                        : Camera2CaptureOptionUnpacker.INSTANCE);

        if (captureType == CaptureType.PREVIEW) {
            mutableConfig.insertOption(OPTION_MAX_RESOLUTION, getPreviewSize());
        }

        int targetRotation = mWindowManager.getDefaultDisplay().getRotation();
        mutableConfig.insertOption(OPTION_TARGET_ROTATION, targetRotation);

        return OptionsBundle.from(mutableConfig);
    }

    /**
     * Returns the device's screen resolution, or 1080p, whichever is smaller.
     */
    @SuppressWarnings("deprecation") /* getDefaultDisplay */
    private Size getPreviewSize() {
        Point displaySize = new Point();
        mWindowManager.getDefaultDisplay().getRealSize(displaySize);

        Size displayViewSize;
        if (displaySize.x > displaySize.y) {
            displayViewSize = new Size(displaySize.x, displaySize.y);
        } else {
            displayViewSize = new Size(displaySize.y, displaySize.x);
        }

        return displayViewSize.getWidth() * displayViewSize.getHeight()
                > MAX_PREVIEW_SIZE.getWidth() * MAX_PREVIEW_SIZE.getHeight() ? MAX_PREVIEW_SIZE
                : displayViewSize;
    }
}
