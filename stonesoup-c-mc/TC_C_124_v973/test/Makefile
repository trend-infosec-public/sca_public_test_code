# Stonesoup makefile.

PROG = main
OBJS = main.o
HDRS = 

include ../../../autotools/sstc.mk

# No changes should be necessary below this line.

VPATH = ../src

all:	$(PROG)

clean:
	-rm $(PROG) $(OBJS)

$(PROG):	$(OBJS)
	$(CC) $(LDFLAGS) -o $(PROG) $(OBJS) $(LDLIBS)

$(OBJS):	$(HDRS)
