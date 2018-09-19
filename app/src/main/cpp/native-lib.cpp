//
// Created by wno-o on 2018-09-19.
//

#include <jni.h>
#include <opencv2/opencv.hpp>
#include <string>
using namespace cv;


extern "C"
JNIEXPORT jstring JNICALL
Java_com_bamboo_bambooheli_activity_BebopActivity_hello_1from_1c(JNIEnv *env, jobject instance) {

    // TODO
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}