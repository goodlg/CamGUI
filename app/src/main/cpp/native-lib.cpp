/* Copyright (c) 2017-2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "Cam-JNI"

#include <jni.h>
#include <string>
#include <android/log.h>
#include <dlfcn.h>

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define IRIS_LIB_PATH "/system/lib/libHWMI.so"

typedef struct {
    int32_t (*open_camera) (uint32_t camId);
    int32_t (*close_camera) ();
    int32_t (*get_num_of_cameras) ();
} mm_camera_ops_t;

typedef struct {
    void *s_handle;
    mm_camera_ops_t *ops;
} mm_camera_module_t;

static JavaVM *m_JVM = NULL;
jclass gIrisClass = NULL;
jobject gIrisJObjectWeak = NULL;
jmethodID gPostEvent = NULL;
mm_camera_ops_t gOps;
mm_camera_module_t gCamContext;

static jint ApiInit(JNIEnv *, jobject) {
    char *error;
    void *s_handle = NULL;
    mm_camera_ops_t *ops = &gOps;
    LOGI("ApiInit");

    //clear prev error
    dlerror();

    s_handle = dlopen(IRIS_LIB_PATH, RTLD_NOW);
    if (NULL == s_handle) {
        LOGE("Failed to get CAM handle in %s()! (Reason=%s)\n", __FUNCTION__, dlerror());
        return JNI_FALSE;
    }

    *(void **) (&ops->open_camera) = dlsym(s_handle, "mm_camera_lib_open");
    if ((error = dlerror()) != NULL)  {
        LOGE("Failed to get mm_camera_lib_open handle in %s()! (Reason=%s)\n", __FUNCTION__, error);
        return JNI_FALSE;
    }

    *(void **) (&ops->close_camera) = dlsym(s_handle, "mm_camera_lib_close");
    if ((error = dlerror()) != NULL)  {
        LOGE("Failed to get mm_camera_lib_close handle in %s()! (Reason=%s)\n", __FUNCTION__, error);
        return JNI_FALSE;
    }

    *(void **) (&ops->get_num_of_cameras) = dlsym(s_handle, "mm_camera_get_num");
    if ((error = dlerror()) != NULL)  {
        LOGE("Failed to get mm_camera_get_num handle in %s()! (Reason=%s)\n", __FUNCTION__, error);
        return JNI_FALSE;
    }

    gCamContext.s_handle = s_handle;
    gCamContext.ops = ops;

    LOGI("ApiInit DONE");
    return JNI_TRUE;
}

static jint ApiDeinit(JNIEnv *, jobject) {
    if (gCamContext.s_handle != NULL) {
        dlclose(gCamContext.s_handle);
        gCamContext.s_handle = NULL;
        gCamContext.ops = NULL;
        LOGI("ApiDeinit DONE");
    }
    return JNI_OK;
}

static jint native_setup(JNIEnv *env, jobject thiz, jobject weak_this, jint camId) {
    LOGI("native_setup (id %d)", camId);
    int ret = -1;
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        // This should never happen
        LOGE(" !!! ERROR !!!");
        return JNI_FALSE;
    }

    gIrisJObjectWeak = env->NewGlobalRef(weak_this);
    gIrisClass = (jclass)env->NewGlobalRef(clazz);

    return gCamContext.ops->open_camera(camId);
}

static jint native_release(JNIEnv *env, jobject thiz) {
    LOGI("release camera");
    if (gIrisJObjectWeak != NULL) {
        env->DeleteGlobalRef(gIrisJObjectWeak);
        gIrisJObjectWeak = NULL;
    }
    if (gIrisClass != NULL) {
        env->DeleteGlobalRef(gIrisClass);
        gIrisClass = NULL;
    }
    return gCamContext.ops->close_camera();
}

static const JNINativeMethod sMethods[] = {
    {"ApiInit", "()I", (void *) ApiInit},
    {"ApiDeinit", "()I", (void *) ApiDeinit},
    {"native_setup", "(Ljava/lang/Object;I)I", (void *) native_setup},
    {"native_release", "()I", (void *) native_release},
};

static int registerNativeMethods(JNIEnv* env, const char* className,
                                 const JNINativeMethod* gMethods, int numMethods) {
    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    gPostEvent = env->GetStaticMethodID(clazz, "postEventFromNative",
                                        "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM *jvm, void *) {
    m_JVM = jvm;
    JNIEnv *env = NULL;
    if (jvm->GetEnv((void**) &env, JNI_VERSION_1_6)) {
        return JNI_ERR;
    }
    if (registerNativeMethods(env, "org/ftd/gyn/camlibrary/MyCamera",
                              sMethods, NELEM(sMethods)) == -1) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
