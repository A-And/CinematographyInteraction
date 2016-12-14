#include <jni.h>
#include <string>

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) !=
        JNI_OK) { return -1; }      // Get jclass with env->FindClass.
    // Register methods with env->RegisterNatives.
    return JNI_VERSION_1_6;
}
extern "C"
jstring
Java_com_ol_andon_reflex_PhotoActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
