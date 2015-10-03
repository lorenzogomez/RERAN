/*******************************************************************************
  * Invokes tcpdump and getevent at the same time.
  * Prints out the pid of each process.
  * TODO: RE-WRITE WITH PTHREADS INSTEAD OF FORK(). GETS MESSY WITH FORKS.
  */

#include <stdio.h>
#include <stdint.h> /* for uint64 definition */
#include <stdlib.h> /* for exit() definition */
#include <time.h>   /* for clock_gettime */
#include <string.h> /*So that the compiler doesn't assume old C-style params*/
#include <unistd.h> /* for execlp */
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define BILLION 1000000000L

int main(int argc, char const *argv[]) {
  struct timespec mono, real_time;
  const uint64_t TIME_FMT = strlen("2012-12-31 12:59:59.123456789") + 1;
  char timestr[TIME_FMT];

  synchronize_time(mono, real_time);

  /* convert real_time into a time of day */
  if (timespec2str(timestr, TIME_FMT, &real_time) != 0) {
    printf("timespec2str failed!\n");
    return 1;
  }
  pid_t tcpdump_thread, getevent_thread, meminfo_thread, cpuinfo_thread, screenshot_thread;
  tcpdump_thread = fork();


  if (tcpdump_thread == 0) {
    /* TCPDump thread */
    char filename[255]; //255 = max filename size

    long sec = mono.tv_sec;
    long nsec = mono.tv_nsec;
    snprintf(filename, sizeof(filename), "%s_%lu.%lu.pcap", timestr, sec, nsec);
    int fd = open(filename, O_RDWR|O_CREAT, S_IRUSR|S_IWUSR);
    dup2(fd, 1);
    dup2(fd, 2);
    close(fd);

    execlp("/data/local/tcpdump", "/data/local/tcpdump", "-i",  "any", "-p",
           "-s", "0", "-w", filename, NULL);
  }
  else {
    //Main Thread
    getevent_thread = fork();
    if (getevent_thread == 0) {
      //GetEvent Thread
      int fd = open("recordedEvents", O_RDWR|O_CREAT, S_IRUSR|S_IWUSR);
      dup2(fd, 1);
      dup2(fd, 2);
      close(fd);
      execlp("getevent", "getevent", "-tt", NULL);
    } else {
      //Main Thread
      meminfo_thread = fork();
      if (meminfo_thread == 0) {
        //Meminfo thread
        int fd = open("meminfo.dat", O_RDWR|O_CREAT, S_IRUSR|S_IWUSR);
        dup2(fd, 1);
        dup2(fd, 2);
        close(fd);
        while (1) {
          synchronize_time(mono, real_time);
          long sec = mono.tv_sec;
          long nsec = mono.tv_nsec;
          char time_output[255];

          if (timespec2str(timestr, TIME_FMT, &real_time) != 0) {
            printf("timespec2str failed!\n");
            return 1;
          }

          snprintf(time_output, sizeof(time_output), "%s\t%lu.%lu", timestr, sec, nsec);
          printf("%s\n", time_output);
          *time_output = '\0';
          execlp("dumpsys", "dumpsys", "meminfo", NULL);
          sleep(500);
        }
      } else {
        //Main Thread
        cpuinfo_thread = fork();
        if (cpuinfo_thread == 0) {
          //CPUInfo thread
          int fd = open("cpuinfo.dat", O_RDWR|O_CREAT, S_IRUSR|S_IWUSR);
          dup2(fd, 1);
          dup2(fd, 2);
          close(fd);
          while (1) {
            synchronize_time(mono, real_time);
            long sec = mono.tv_sec;
            long nsec = mono.tv_nsec;
            char time_output[255];

            if (timespec2str(timestr, TIME_FMT, &real_time) != 0) {
              printf("timespec2str failed!\n");
              return 1;
            }
            snprintf(time_output, sizeof(time_output), "%s\t%lu.%lu", timestr, sec, nsec);
            printf("%s\n", time_output);
            *time_output = '\0';
            execlp("dumpsys", "dumpsys", "cpuinfo",  NULL);
            sleep(500);
          }
        } else {
          //Main Thead
          screenshot_thread = fork();
          if (screenshot_thread == 0) {
            //Screenshot thread
            int i = 0;
            while (1) {
              char filename[255];
              snprintf(filename, sizeof(filename), "/data/local/screenshots/screen%d.png", i);
              execlp("screencap", "screencap", "-p", filename, NULL);
              sleep(500);
              i++;
            }
          } else {
            //Main Thread
            int user_signal = -1;
            printf("Type 0 to end measuring.\n");
            scanf("%d", &user_signal);

            if (!user_signal) {
              terminate_child(meminfo_thread);
              terminate_child(cpuinfo_thread);
              terminate_child(tcpdump_thread);
              terminate_child(getevent_thread);
              terminate_child(screenshot_thread);
            }
          }
        }
      }
    }
  }
  return 0;
}

void synchronize_time(struct timespec mono, struct timespec real_time) {
  /* measure monotonic time */
  clock_gettime(CLOCK_MONOTONIC, &mono);

  /* measure human-readable time */
  clock_gettime(CLOCK_REALTIME, &real_time);
}

void terminate_child(pid_t child_pid) {
  kill(child_pid, SIGTERM);
}

void kill_child(pid_t child_pid) {
  kill(child_pid, SIGKILL);
}

int timespec2str(char *buf, uint64_t len, struct timespec *ts) {
    int ret;
    struct tm t;

    tzset();
    if (localtime_r(&(ts->tv_sec), &t) == NULL)
        return 1;

    ret = strftime(buf, len, "%F %T", &t);
    if (ret == 0)
        return 2;
    len -= ret - 1;

    ret = snprintf(&buf[strlen(buf)], len, ".%09ld", ts->tv_nsec);
    if (ret >= len)
        return 3;

    return 0;
}
