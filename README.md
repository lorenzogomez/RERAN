
RERAN
=====
Record and Replay for Android

(c)  Copyright 2011-2013

The preferred license for RERAN is the BSD License.

####Getting Started

Getting started with RERAN is easy. The instructions below assume you
have the Android SDK installed on your computer (runs on Linux, Mac, 
and Windows). We use the adb debugging bridge to push files onto the 
phone and run the record and replay commands. The ./adb tool is in the 
/platform-tools folder of the SDK folder you install. The example below 
performs our standard record and replay. For the selective replay and 
time-warping features, please see their respective pages.

####RERAN Design

First, recording with getevent will create a log of the events used 
during the run, e.g., recordedEvents.txt. Second, send the recorded 
log into the Translate program. The Translate program will output a 
translated log of the original events. Third, push the translated 
log, e.g., translatedEvents.txt, onto the phone. Fourth, run the Replay 
program using the adb shell.


####ARM Cross-compiler

In order for the replay program to run on Android devices, they must be compiled using a cross-compiler for ARM CPU's. If you already have an ARM cross-compiler on your computer, then you are ready to go. If not, please find one that works with your operating system. The executable contained in our release was compiled on Linux. From our experience, finding and installing an ARM cross-compiler for Mac was difficult; it is possible, but included many hacks to get going, and is not recommended. 

We would recommend using Sourcery CodeBench for ARM Lite on Linux. 
https://sourcery.mentor.com/GNUToolchain/release2450

After the ARM cross-compiler is installed, you can compile the source code using the compiler's version of gcc, shown below.
```    
  arm-none-linux-gnueabi-gcc -static -o replay replay.c
```

####Running Example

Push replay tool onto the phone: "/data/local" will be our local 
directory on the phone for the RERAN files. If it does not exist, it 
will be created. This step only needs to be done once.
```
    cd /path/to/android-sdk/platform-tools
    
    ./adb push ./replay /data/local
```    

Record a trace: The getevent tool is part of the Android SDK. The "-tt" 
flag is to timestamp each event (used by the Translator in the next step).
```
    ./adb shell getevent -tt > recordedEvents.txt
```    

Run the Translate program: The first two arguments of the Translate 
program are the path to the recorded events and the name of the translated 
events to output, respectively. There are also extra flags: see selective 
replay and time-warping.
```
    cd /path/to/translate.jar/
    
    java -jar translate.jar /path/to/recordedEvents.txt /path/to/android-sdk/platform-tools/translatedEvents.txt
```    

Push the translated recorded events onto the phone:
```
    ./adb push translatedEvents.txt /data/local
```    

Run the replay program (after your app is setup): See setting up your 
app for more info.
```
    ./adb shell /data/local/./replay /data/local/translatedEvents.txt
```    

Please see the website www.androidreran.com for more info.