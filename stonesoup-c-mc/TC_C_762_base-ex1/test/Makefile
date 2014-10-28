# Generic STONESOUP makefile for C test case projects.
#
# Author: John D. Ramsdell (ramsdell)
# Date: 3/20/2012

# Place this directory in the Release directory of a C test case
# project.

# This makefile obtains compilation platform information via two
# methods.  When SSTC_INCLUDE is defined, it includes the file named
# by the variable.  The file should be a makefile fragment generated
# using a configure script run on the machine used to compile the
# project.  Otherwise, it obtains information from environment
# variables that begin with SC_, however, the default values should
# work in most cases.

# Ideally, this whole section would be replaced by the include of a
# makefile fragment given by a relative path.  All compilation
# platform specific information would always be maintained at that
# location.

ifdef SSTC_INCLUDE
# Get platform information from makefile fragment
include $(SSTC_INCLUDE)
else
# Otherwise
# Set CC variable
ifdef SS_CC
CC = $(SS_CC)
else
# Use C99 and GCC by default
CC = gcc -std=gnu99
endif
# Set CFLAGS
ifdef SS_CFLAGS
CFLAGS = $(SS_CFLAGS)
else
CFLAGS = -O0 -fno-stack-protector -z execstack -Wall
endif
# Set linker library information
ifdef SS_LDFLAGS
# Some of the information in the SS_LDFLAGS should not be there.
# Library names such as -lssl should be in LDLIBS.
LDFLAGS = $(SS_LDFLAGS) $(SS_OS_SPECIFIC_LDFLAGS)
else
LDLIBS = -lXtst -lXrandr -lX11 -lssl
endif
endif

# The program name is derived from the project directory.
PROG := $(notdir $(shell cd ..; pwd))
# Use all the C sources and headers in the ../src directory
OBJS := $(notdir $(patsubst %.c,%.o,$(wildcard ../src/*.c)))
HDRS := $(notdir $(wildcard ../src/*.h))

VPATH = ../src

all:	$(PROG)

clean:
	-rm $(PROG) $(OBJS)

$(PROG):	$(OBJS)
	$(CC) $(LDFLAGS) -o $(PROG) $(OBJS) $(LDLIBS)

$(OBJS):	$(HDRS)
