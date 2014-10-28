
SHELL=/bin/sh

# To assist in cross-compiling
CC=gcc
AR=ar
RANLIB=ranlib
LDFLAGS=

#BIGFILES=-D_FILE_OFFSET_BITS=64
BIGFILES=-D_FILE_OFFSET_BITS=32
CFLAGS=-Wall -Winline -O -g $(BIGFILES)

# Where you want it installed when you do 'make install'
PREFIX=/usr


OBJS= blocksort.o  \
      huffman.o    \
      crctable.o   \
      randtable.o  \
      compress.o   \
      decompress.o \
      bzlib.o

#all: libbz2.a bzip2 bzip2recover test
all: libbz2.a bzip2 bzip2recover 

bzip2: libbz2.a bzip2.o
	$(SS_LNK) $(CFLAGS) $(SS_CFLAGS) $(LDFLAGS) $(SS_LDFLAGS) -o bzip2 bzip2.o -L. -lbz2
#	$(CC) $(CFLAGS) $(LDFLAGS) -o bzip2 bzip2.o -L. -lbz2

bzip2recover: bzip2recover.o
	$(SS_LNK) $(CFLAGS) $(SS_CFLAGS) $(LDFLAGS) $(SS_LDFLAGS) -o bzip2recover bzip2recover.o
#	$(CC) $(CFLAGS) $(LDFLAGS) -o bzip2recover bzip2recover.o

libbz2.a: $(OBJS)
	rm -f libbz2.a
	$(AR) cq libbz2.a $(OBJS)
	@if ( test -f $(RANLIB) -o -f /usr/bin/ranlib -o \
		-f /bin/ranlib -o -f /usr/ccs/bin/ranlib ) ; then \
		echo $(RANLIB) libbz2.a ; \
		$(RANLIB) libbz2.a ; \
	fi

check: test

clean: 
	rm -f *.o libbz2.a bzip2 bzip2recover \
	sample1.rb2 sample2.rb2 sample3.rb2 \
	sample1.tst sample2.tst sample3.tst

blocksort.o: blocksort.c
	@cat words0
	#$(CC) $(CFLAGS) -c blocksort.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c blocksort.c
huffman.o: huffman.c
	#$(CC) $(CFLAGS) -c huffman.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c huffman.c
crctable.o: crctable.c
	#$(CC) $(CFLAGS) -c crctable.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c crctable.c
randtable.o: randtable.c
	#$(CC) $(CFLAGS) -c randtable.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c randtable.c
compress.o: compress.c
	#$(CC) $(CFLAGS) -c compress.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c compress.c
decompress.o: decompress.c
	#$(CC) $(CFLAGS) -c decompress.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c decompress.c
bzlib.o: bzlib.c
	#$(CC) $(CFLAGS) -c bzlib.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c bzlib.c
bzip2.o: bzip2.c
	#$(CC) $(CFLAGS) -c bzip2.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c bzip2.c
bzip2recover.o: bzip2recover.c
	#$(CC) $(CFLAGS) -c bzip2recover.c
	$(SS_CC) $(CFLAGS) $(SS_CFLAGS) -c bzip2recover.c
