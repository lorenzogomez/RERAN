_________
Components:
  - tcpdump & libpcap
  - androidreran

__________


####Getting Started

Getting started is easy. Push the binaries to the android device's /data/local folder.

   ```
   adb push bin/* /data/local/
   ```


 Once the binaries are present, run the begin_measuring binary.

It spawns two threads, one that measures network connectivity, and another that captures touch events. Upon running, the program will print the IDs of the two threads, and when you are done recording, kill these two threads. The output files (recordedEvents, [timestamp].pcap) can now be pulled using adb.

Run the CleanRecordedEvents java program to clean the output.

```
java CleanRecordedEvents recordedEvents
```

The output can now be processed using the translate java program.



  ```
  java Translate recordedEvents.dat translatedEvents.txt
  adb push translatedEvents.txt /data/local/
  ```

To replay the touch events, call the replay program

```
adb shell /data/local/./replay /data/local/translatedEvents.txt
```
