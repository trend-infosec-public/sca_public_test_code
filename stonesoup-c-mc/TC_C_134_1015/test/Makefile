# Stonesoup makefile template.  Replace <> with project specific values

PROG = cmdInterpreter
OBJS = cmdInterpreter.o getInt.o
HDRS = getInt.h

# Replace <> with a path to sstc.mk, something like:
# include ../../../autotools/sstc.mk
include ../../../autotools/sstc.mk

# No changes should be necessary below this line.

VPATH = ../src

all:	$(PROG)

clean:
	rm $(PROG) $(OBJS)

$(PROG):	$(OBJS)
	$(CC) $(LDFLAGS) -o $(PROG) $(OBJS) $(LDLIBS)

$(OBJS):	$(HDRS)
