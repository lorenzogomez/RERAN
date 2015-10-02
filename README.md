_________
Components:
  - tcpdump & libpcap
  - androidreran

__________


####Getting Started

Getting started is easy. Push the binaries to the android device's /data/local folder.

   ```
   adb push bin/ /data/local/
   ```


 Once the binaries are present, run the begin_measuring binary.

It spawns several threads: one that measures network connectivity, one that captures touch events, one that measures memory status, one that measures activity on the processor, and one that periodically takes screenshots. The output files (recordedEvents, [timestamp].pcap, cpuinfo.dat, meminfo.dat, screenshots/*) can now be pulled using adb.

The output can now be processed using the translate java program.

  ```
  java Translate recordedEvents.dat translatedEvents.txt
  adb push translatedEvents.txt /data/local/
  ```

To replay the touch events, call the replay program

```
adb shell /data/local/./replay /data/local/translatedEvents.txt
```
