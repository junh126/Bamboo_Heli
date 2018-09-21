//
// Created by wno-o on 2018-09-19.
//
/*
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <string>
#include <iostream>
using namespace cv;
using namespace std;


extern "C"
JNIEXPORT jstring JNICALL
Java_com_bamboo_bambooheli_activity_BebopActivity_hello_1from_1c(JNIEnv *env, jobject instance) {

    // TODO
    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}*/
#include <jni.h>
#include <iostream>
#include <string>
#include <cstdlib>
#include <opencv/highgui.h>
#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_bamboo_bambooheli_activity_Carplate_car_1plate(JNIEnv *env, jobject instance, jstring imgsrc1) {
    const jclass stringClass = env->GetObjectClass(imgsrc1);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(imgsrc1, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    string imgsrc = string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);

    Mat image, image2, image3, drawing;  //  Make images.
    Rect rect, temp_rect;  //  Make temporary rectangles.
    vector<vector<Point> > contours;  //  Vectors for 'findContours' function.
    vector<Vec4i> hierarchy;

    double ratio, delta_x, delta_y, gradient;  //  Variables for 'Snake' algorithm.
    int select, plate_width, count, friend_count = 0, refinery_count = 0;


    image = imread(imgsrc);  //  Load an image file.



    image.copyTo(image2);  //  Copy to temporary images.
    image.copyTo(image3);  //  'image2' - to preprocessing, 'image3' - to 'Snake' algorithm.

    cvtColor(image2, image2, CV_BGR2GRAY);  //  Convert to gray image.


    Canny(image2, image2, 100, 300, 3);  //  Getting edges by Canny algorithm.


    //  Finding contours.
    findContours(image2, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point());
    vector<vector<Point> > contours_poly(contours.size());
    vector<Rect> boundRect(contours.size());
    vector<Rect> boundRect2(contours.size());

    //  Bind rectangle to every rectangle.
    for (int i = 0; i< contours.size(); i++) {
        approxPolyDP(Mat(contours[i]), contours_poly[i], 1, true);
        boundRect[i] = boundingRect(Mat(contours_poly[i]));
    }

    drawing = Mat::zeros(image2.size(), CV_8UC3);

    for (int i = 0; i< contours.size(); i++) {

        ratio = (double)boundRect[i].height / boundRect[i].width;

        //  Filtering rectangles height/width ratio, and size.
        if ((ratio <= 2.5) && (ratio >= 0.5) && (boundRect[i].area() <= 700) && (boundRect[i].area() >= 100)) {

            drawContours(drawing, contours, i, Scalar(0, 255, 255), 1, 8, hierarchy, 0, Point());
            rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), Scalar(255, 0, 0), 1, 8, 0);

            //  Include only suitable rectangles.
            boundRect2[refinery_count] = boundRect[i];
            refinery_count += 1;
        }
    }

    boundRect2.resize(refinery_count);  //  Resize refinery rectangle array.

    //  Bubble Sort accordance with X-coordinate.
    for (int i = 0; i<boundRect2.size(); i++) {
        for (int j = 0; j<(boundRect2.size() - i); j++) {
            if (boundRect2[j].tl().x > boundRect2[j + 1].tl().x) {
                temp_rect = boundRect2[j];
                boundRect2[j] = boundRect2[j + 1];
                boundRect2[j + 1] = temp_rect;
            }
        }
    }


    for (int i = 0; i< boundRect2.size(); i++) {

        rectangle(image3, boundRect2[i].tl(), boundRect2[i].br(), Scalar(0, 255, 0), 1, 8, 0);

        count = 0;

        //  Snake moves to right, for eating his freind.
        for (int j = i + 1; j<boundRect2.size(); j++) {

            delta_x = abs(boundRect2[j].tl().x - boundRect2[i].tl().x);

            if (delta_x > 150)  //  Can't eat snake friend too far ^-^.
                break;

            delta_y = abs(boundRect2[j].tl().y - boundRect2[i].tl().y);


            //  If delta length is 0, it causes a divide-by-zero error.
            if (delta_x == 0) {
                delta_x = 1;
            }

            if (delta_y == 0) {
                delta_y = 1;
            }


            gradient = delta_y / delta_x;  //  Get gradient.
            cout << gradient << endl;

            if (gradient < 0.25) {  //  Can eat friends only on straight line.
                count += 1;
            }
        }

        //  Find the most full snake.
        if (count > friend_count) {
            select = i;  //  Save most full snake number.
            friend_count = count;  //  Renewal number of friends hunting.
            rectangle(image3, boundRect2[select].tl(), boundRect2[select].br(), Scalar(255, 0, 0), 1, 8, 0);
            plate_width = delta_x;  //  Save the last friend ate position.
        }                           //  It's similar to license plate width, Right?
    }

    //  Drawing most full snake friend on the image.
    rectangle(image3, boundRect2[select].tl(), boundRect2[select].br(), Scalar(0, 0, 255), 2, 8, 0);
    line(image3, boundRect2[select].tl(), Point(boundRect2[select].tl().x + plate_width, boundRect2[select].tl().y), Scalar(0, 0, 255), 1, 8, 0);

    //imwrite("/home/song/Downloads/license_plate/Plates/number/1-1.JPG",
      //         image(Rect(boundRect2[select].tl().x-20, boundRect2[select].tl().y-20, plate_width+40, plate_width*0.3)));


}
extern "C"
JNIEXPORT void JNICALL
Java_com_bamboo_bambooheli_activity_ManualActivity_car_1plate(JNIEnv *env, jobject instance,
                                                              jlong addrinput, jlong addroutput) {

    Mat  image2, image3, drawing;  //  Make images.
    Rect rect, temp_rect;  //  Make temporary rectangles.
    vector<vector<Point> > contours;  //  Vectors for 'findContours' function.
    vector<Vec4i> hierarchy;

    double ratio, delta_x, delta_y, gradient;  //  Variables for 'Snake' algorithm.
    int select, plate_width, count, friend_count = 0, refinery_count = 0;


    Mat &image = *(Mat *) addrinput;
    Mat &resimage = *(Mat *) addroutput;

    image.copyTo(image2);  //  Copy to temporary images.
    image.copyTo(image3);  //  'image2' - to preprocessing, 'image3' - to 'Snake' algorithm.

    cvtColor(image2, image2, CV_BGR2GRAY);  //  Convert to gray image.


    Canny(image2, image2, 100, 300, 3);  //  Getting edges by Canny algorithm.


    //  Finding contours.
    findContours(image2, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point());
    vector<vector<Point> > contours_poly(contours.size());
    vector<Rect> boundRect(contours.size());
    vector<Rect> boundRect2(contours.size());

    //  Bind rectangle to every rectangle.
    for (int i = 0; i< contours.size(); i++) {
        approxPolyDP(Mat(contours[i]), contours_poly[i], 1, true);
        boundRect[i] = boundingRect(Mat(contours_poly[i]));
    }

    drawing = Mat::zeros(image2.size(), CV_8UC3);

    for (int i = 0; i< contours.size(); i++) {

        ratio = (double)boundRect[i].height / boundRect[i].width;

        //  Filtering rectangles height/width ratio, and size.
        if ((ratio <= 2.5) && (ratio >= 0.5) && (boundRect[i].area() <= 700) && (boundRect[i].area() >= 100)) {

            drawContours(drawing, contours, i, Scalar(0, 255, 255), 1, 8, hierarchy, 0, Point());
            rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), Scalar(255, 0, 0), 1, 8, 0);

            //  Include only suitable rectangles.
            boundRect2[refinery_count] = boundRect[i];
            refinery_count += 1;
        }
    }

    boundRect2.resize(refinery_count);  //  Resize refinery rectangle array.

    //  Bubble Sort accordance with X-coordinate.
    for (int i = 0; i<boundRect2.size(); i++) {
        for (int j = 0; j<(boundRect2.size() - i); j++) {
            if (boundRect2[j].tl().x > boundRect2[j + 1].tl().x) {
                temp_rect = boundRect2[j];
                boundRect2[j] = boundRect2[j + 1];
                boundRect2[j + 1] = temp_rect;
            }
        }
    }


    for (int i = 0; i< boundRect2.size(); i++) {

        rectangle(image3, boundRect2[i].tl(), boundRect2[i].br(), Scalar(0, 255, 0), 1, 8, 0);

        count = 0;

        //  Snake moves to right, for eating his freind.
        for (int j = i + 1; j<boundRect2.size(); j++) {

            delta_x = abs(boundRect2[j].tl().x - boundRect2[i].tl().x);

            if (delta_x > 150)  //  Can't eat snake friend too far ^-^.
                break;

            delta_y = abs(boundRect2[j].tl().y - boundRect2[i].tl().y);


            //  If delta length is 0, it causes a divide-by-zero error.
            if (delta_x == 0) {
                delta_x = 1;
            }

            if (delta_y == 0) {
                delta_y = 1;
            }


            gradient = delta_y / delta_x;  //  Get gradient.
            cout << gradient << endl;

            if (gradient < 0.25) {  //  Can eat friends only on straight line.
                count += 1;
            }
        }

        //  Find the most full snake.
        if (count > friend_count) {
            select = i;  //  Save most full snake number.
            friend_count = count;  //  Renewal number of friends hunting.
            rectangle(image3, boundRect2[select].tl(), boundRect2[select].br(), Scalar(255, 0, 0), 1, 8, 0);
            plate_width = delta_x;  //  Save the last friend ate position.
        }                           //  It's similar to license plate width, Right?
    }

    //  Drawing most full snake friend on the image.
    rectangle(image3, boundRect2[select].tl(), boundRect2[select].br(), Scalar(0, 0, 255), 2, 8, 0);
    line(image3, boundRect2[select].tl(), Point(boundRect2[select].tl().x + plate_width, boundRect2[select].tl().y), Scalar(0, 0, 255), 1, 8, 0);

    image(Rect(boundRect2[select].tl().x - 20, boundRect2[select].tl().y - 20, plate_width + 40, plate_width*0.3)).copyTo(resimage);

}