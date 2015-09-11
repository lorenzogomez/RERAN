/*******************************************************************************
 * Author: Hesham Salman, 2015
 * Invokes tcpdump and getevent at the same time.
 * Prints out the pid of each process.
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
  int i;
  const uint64_t TIME_FMT = strlen("2012-12-31 12:59:59.123456789") + 1;
  char timestr[TIME_FMT];

  /* measure monotonic time */
  clock_gettime(CLOCK_MONOTONIC, &mono);

  /* measure human-readable time */
  clock_gettime(CLOCK_REALTIME, &real_time);

  /* convert real_time into a time of day */
  if (timespec2str(timestr, TIME_FMT, &real_time) != 0) {
    printf("timespec2str failed!\n");
    return 1;
  } else {
    pid_t child_a, child_b; //child_a = tcpdump, child_b = getevent

    child_a = fork();

    if (child_a == 0) {
      /* TCPDump thread */
      printf("%d\n", getpid()); //Print PID of tcpdump
      char filename[255]; //255 = max filename size
      strcpy(filename,timestr);
      strcat(filename,"_"); /* the _ separates monotonic and real times */

      long sec = mono.tv_sec;
      long nsec = mono.tv_nsec;

      const int n = snprintf(NULL, 0, "%lu", sec);
      char secbuf[n+1];
      snprintf(secbuf, n+1, "%lu", sec);

      const int n2 = snprintf(NULL, 0, "%lu", nsec);
      char nsecbuf[n2 + 1];
      snprintf(nsecbuf, n2 + 1, "%lu", nsec);
      strcat(filename, secbuf);
      strcat(filename, ".");
      strcat(filename, nsecbuf);
      strcat(filename, ".pcap");

      execlp("/data/local/tcpdump", "/data/local/tcpdump", "-i",  "any", "-p",
             "-s", "0", "-w", filename, NULL);
    } else {
      child_b = fork();
      if (child_b == 0) {
        int fd = open("recordedEvents", O_RDWR|O_CREAT, S_IRUSR|S_IWUSR);
        printf("%d\n", getpid()); //print PID of getevent
        dup2(fd, 1);
        close(fd);
        execlp("getevent", "getevent", "-tt", NULL);
      }
    }
  }
  return 0;
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
