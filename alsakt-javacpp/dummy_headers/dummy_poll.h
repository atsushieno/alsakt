#define EINVAL 22
#define O_RDONLY 00
#define O_WRONLY 01
#define O_RDWR 02
#define O_NONBLOCK 04000
#define	__LITTLE_ENDIAN	1234

typedef unsigned long int nfds_t;

extern "C" struct pollfd
  {
    int fd;
    short int events;
    short int revents;
  };

extern "C" int poll (struct pollfd *__fds, nfds_t __nfds, int __timeout);
