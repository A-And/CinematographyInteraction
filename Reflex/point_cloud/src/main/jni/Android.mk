#
# Copyright 2014 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)
PROJECT_ROOT_FROM_JNI:= ../../../../..
PROJECT_ROOT:= $(call my-dir)/../../../../..

include $(CLEAR_VARS)
LOCAL_MODULE    := libcpp_point_cloud_example
LOCAL_SHARED_LIBRARIES := tango_client_api tango_support_api
LOCAL_CFLAGS    := -std=c++11

LOCAL_C_INCLUDES := $(PROJECT_ROOT)/tango-service-sdk/include/ \
                    $(PROJECT_ROOT)/tango_gl/include \
                    $(PROJECT_ROOT)/third_party/glm/

LOCAL_SRC_FILES := jni_interface.cc \
                   point_cloud_drawable.cc \
                   point_cloud_app.cc \
                   scene.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/axis.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/camera.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/conversions.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/drawable_object.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/frustum.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/gesture_camera.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/grid.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/line.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/shaders.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/trace.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/transform.cc \
                   $(PROJECT_ROOT_FROM_JNI)/tango_gl/util.cc

LOCAL_LDLIBS    := -llog -lGLESv2 -L$(SYSROOT)/usr/lib
include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(PROJECT_ROOT))
$(call import-module,tango_client_api)
$(call import-module,tango_support_api)
