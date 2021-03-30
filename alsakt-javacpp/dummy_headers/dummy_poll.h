typedef unsigned long int nfds_t;

struct pollfd
  {
    int fd;
    short int events;
    short int revents;
  };

int poll (struct pollfd *__fds, nfds_t __nfds, int __timeout);

