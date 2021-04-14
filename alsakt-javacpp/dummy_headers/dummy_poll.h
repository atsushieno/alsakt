typedef unsigned long int nfds_t;

extern "C" struct pollfd
  {
    int fd;
    short int events;
    short int revents;
  };

extern "C" int poll (struct pollfd *__fds, nfds_t __nfds, int __timeout);
