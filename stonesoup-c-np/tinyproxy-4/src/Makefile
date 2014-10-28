
SHELL = /bin/sh

srcdir = .
top_srcdir = ..
#prefix = /usr/local
#exec_prefix = ${prefix}

#bindir = ${exec_prefix}/bin
#sbindir = ${exec_prefix}/sbin
#libexecdir = ${exec_prefix}/libexec
#datadir = ${prefix}/share
#sysconfdir = ${prefix}/etc
#sharedstatedir = ${prefix}/com
#localstatedir = ${prefix}/var
#libdir = ${exec_prefix}/lib
#infodir = ${prefix}/info
#mandir = ${prefix}/man
#includedir = ${prefix}/include
#oldincludedir = /usr/include

DESTDIR =

#pkgdatadir = $(datadir)/tinyproxy
#pkglibdir = $(libdir)/tinyproxy
#pkgincludedir = $(includedir)/tinyproxy

top_builddir = ..

#ACLOCAL = aclocal
#AUTOCONF = autoconf
#AUTOMAKE = automake
#AUTOHEADER = autoheader

#INSTALL = /usr/bin/install -c
#INSTALL_PROGRAM = ${INSTALL} $(AM_INSTALL_PROGRAM_FLAGS)
#INSTALL_DATA = ${INSTALL} -m 644
#INSTALL_SCRIPT = ${INSTALL_PROGRAM}
#transform = s,x,x,

#NORMAL_INSTALL = :
#PRE_INSTALL = :
#POST_INSTALL = :
#NORMAL_UNINSTALL = :
#PRE_UNINSTALL = :
#POST_UNINSTALL = :
#build_alias = i686-pc-linux-gnu
#build_triplet = i686-pc-linux-gnu
#host_alias = i686-pc-linux-gnu
#host_triplet = i686-pc-linux-gnu
#target_alias = i686-pc-linux-gnu
#target_triplet = i686-pc-linux-gnu
AWK = gawk
CC = gcc
CFLAGS = -DNDEBUG -O2 -D_REENTRANT 
CPP = gcc -E
CPPFLAGS = 
LEX = flex
LIBOBJS = filter.o 
LIBS = -lresolv -lnsl -lpthread 
LN_S = ln -s
#MAKEINFO = /home/swang/real_world_test_cases/phase-1/tinyproxy-4_testCase/tinyproxy-4/missing makeinfo
PACKAGE = tinyproxy
U = 
VERSION = 1.4.3
YACC = bison -y

YFLAGS = -d
LDFLAGS = 

bin_PROGRAMS = tinyproxy

tinyproxy_SOURCES =  	conns.c conns.h 	buffer.c buffer.h 	dnscache.c dnscache.h 	log.c log.h 	reqs.c reqs.h 	sock.c sock.h 	tinyproxy.c tinyproxy.h 	utils.c utils.h 	anonymous.c anonymous.h 	stats.c stats.h 	thread.c thread.h 	grammar.y scanner.l 	acl.c acl.h 	ternary.c ternary.h 	regexp.h


tinyproxy_LDADD = filter.o 

EXTRA_DIST = gnuregex.c gnuregex.h 	filter.c filter.h


EXTRA_tinyproxy_SOURCES = grammar.h
mkinstalldirs = $(SHELL) $(top_srcdir)/mkinstalldirs
CONFIG_HEADER = ../config.h
CONFIG_CLEAN_FILES = 
PROGRAMS =  $(bin_PROGRAMS)


DEFS = -DHAVE_CONFIG_H -I. -I$(srcdir) -I..
tinyproxy_OBJECTS =  conns.o buffer.o dnscache.o log.o reqs.o sock.o \
tinyproxy.o utils.o anonymous.o stats.o thread.o grammar.o scanner.o \
acl.o ternary.o
tinyproxy_DEPENDENCIES =  filter.o 
tinyproxy_LDFLAGS = 
LEX_OUTPUT_ROOT = lex.yy
LEXLIB = -lfl
#COMPILE = $(CC) $(DEFS) $(INCLUDES) $(AM_CPPFLAGS) $(CPPFLAGS) $(AM_CFLAGS) $(CFLAGS)
COMPILE = $(SS_CC) $(DEFS) $(INCLUDES) $(AM_CPPFLAGS) $(CPPFLAGS) $(AM_CFLAGS) $(CFLAGS) $(SS_CFLAGS)
#CCLD = $(CC)
CCLD = $(SS_CC)
#LINK = $(CCLD) $(AM_CFLAGS) $(CFLAGS) $(LDFLAGS) -o $@
LINK = $(SS_LNK) $(AM_CFLAGS) $(CFLAGS) $(SS_CFLAGS) $(LDFLAGS) $(SS_LDFLAGS) -o $@
DIST_COMMON =  Makefile.am Makefile.in filter.c gnuregex.c grammar.c \
scanner.c


DISTFILES = $(DIST_COMMON) $(SOURCES) $(HEADERS) $(TEXINFOS) $(EXTRA_DIST)

TAR = tar
GZIP_ENV = --best
SOURCES = $(tinyproxy_SOURCES) $(EXTRA_tinyproxy_SOURCES)
OBJECTS = $(tinyproxy_OBJECTS)

all: all-redirect
.SUFFIXES:
.SUFFIXES: .S .c .l .o .s .y
$(srcdir)/Makefile.in: Makefile.am $(top_srcdir)/configure.in $(ACLOCAL_M4) 
	cd $(top_srcdir) && $(AUTOMAKE) --gnu --include-deps src/Makefile

Makefile: $(srcdir)/Makefile.in  $(top_builddir)/config.status
	cd $(top_builddir) \
	  && CONFIG_FILES=$(subdir)/$@ CONFIG_HEADERS= $(SHELL) ./config.status


#mostlyclean-binPROGRAMS:

clean-binPROGRAMS:
	-test -z "$(bin_PROGRAMS)" || rm -f $(bin_PROGRAMS)

#distclean-binPROGRAMS:

#maintainer-clean-binPROGRAMS:

#install-binPROGRAMS: $(bin_PROGRAMS)
#	@$(NORMAL_INSTALL)
#	$(mkinstalldirs) $(DESTDIR)$(bindir)
#	@list='$(bin_PROGRAMS)'; for p in $$list; do \
#	  if test -f $$p; then \
#	    echo "  $(INSTALL_PROGRAM) $$p $(DESTDIR)$(bindir)/`echo $$p|sed 's/$(EXEEXT)$$//'|sed '$(transform)'|sed 's/$$/$(EXEEXT)/'`"; \
#	     $(INSTALL_PROGRAM) $$p $(DESTDIR)$(bindir)/`echo $$p|sed 's/$(EXEEXT)$$//'|sed '$(transform)'|sed 's/$$/$(EXEEXT)/'`; \
#	  else :; fi; \
#	done
#
#uninstall-binPROGRAMS:
#	@$(NORMAL_UNINSTALL)
#	list='$(bin_PROGRAMS)'; for p in $$list; do \
#	  rm -f $(DESTDIR)$(bindir)/`echo $$p|sed 's/$(EXEEXT)$$//'|sed '$(transform)'|sed 's/$$/$(EXEEXT)/'`; \
#	done

.c.o:
	$(COMPILE) -c $<

.s.o:
	$(COMPILE) -c $<

.S.o:
	$(COMPILE) -c $<

mostlyclean-compile:
	-rm -f *.o core *.core

clean-compile:

distclean-compile:
	-rm -f *.tab.c

maintainer-clean-compile:

tinyproxy: $(tinyproxy_OBJECTS) $(tinyproxy_DEPENDENCIES)
	@rm -f tinyproxy
	$(LINK) $(tinyproxy_LDFLAGS) $(tinyproxy_OBJECTS) $(tinyproxy_LDADD) $(LIBS)
.l.c:
	$(LEX) $(AM_LFLAGS) $(LFLAGS) $< && mv $(LEX_OUTPUT_ROOT).c $@
.y.c:
	$(YACC) $(AM_YFLAGS) $(YFLAGS) $< && mv y.tab.c $*.c
	if test -f y.tab.h; then \
	if cmp -s y.tab.h $*.h; then rm -f y.tab.h; else mv y.tab.h $*.h; fi; \
	else :; fi
grammar.h: grammar.c


tags: TAGS

ID: $(HEADERS) $(SOURCES) $(LISP)
	list='$(SOURCES) $(HEADERS)'; \
	unique=`for i in $$list; do echo $$i; done | \
	  awk '    { files[$$0] = 1; } \
	       END { for (i in files) print i; }'`; \
	here=`pwd` && cd $(srcdir) \
	  && mkid -f$$here/ID $$unique $(LISP)

TAGS:  $(HEADERS) $(SOURCES)  $(TAGS_DEPENDENCIES) $(LISP)
	tags=; \
	here=`pwd`; \
	list='$(SOURCES) $(HEADERS)'; \
	unique=`for i in $$list; do echo $$i; done | \
	  awk '    { files[$$0] = 1; } \
	       END { for (i in files) print i; }'`; \
	test -z "$(ETAGS_ARGS)$$unique$(LISP)$$tags" \
	  || (cd $(srcdir) && etags $(ETAGS_ARGS) $$tags  $$unique $(LISP) -o $$here/TAGS)

mostlyclean-tags:

clean-tags:

distclean-tags:
	-rm -f TAGS ID

maintainer-clean-tags:

distdir = $(top_builddir)/$(PACKAGE)-$(VERSION)/$(subdir)

subdir = src

distdir: $(DISTFILES)
	@for file in $(DISTFILES); do \
	  d=$(srcdir); \
	  if test -d $$d/$$file; then \
	    cp -pr $$d/$$file $(distdir)/$$file; \
	  else \
	    test -f $(distdir)/$$file \
	    || ln $$d/$$file $(distdir)/$$file 2> /dev/null \
	    || cp -p $$d/$$file $(distdir)/$$file || :; \
	  fi; \
	done
acl.o: acl.c tinyproxy.h ../config.h acl.h log.h sock.h utils.h conns.h
anonymous.o: anonymous.c tinyproxy.h ../config.h anonymous.h ternary.h \
	log.h
buffer.o: buffer.c tinyproxy.h ../config.h buffer.h log.h utils.h \
	conns.h
conns.o: conns.c tinyproxy.h ../config.h buffer.h conns.h stats.h \
	utils.h
dnscache.o: dnscache.c tinyproxy.h ../config.h dnscache.h log.h \
	ternary.h utils.h conns.h
filter.o: filter.c tinyproxy.h ../config.h filter.h regexp.h utils.h \
	conns.h
gnuregex.o: gnuregex.c ../config.h gnuregex.h
grammar.o: grammar.c tinyproxy.h ../config.h acl.h anonymous.h ternary.h \
	log.h thread.h
log.o: log.c tinyproxy.h ../config.h log.h
reqs.o: reqs.c tinyproxy.h ../config.h acl.h anonymous.h ternary.h \
	buffer.h conns.h filter.h log.h regexp.h reqs.h sock.h stats.h \
	utils.h
scanner.o: scanner.c tinyproxy.h ../config.h grammar.h
sock.o: sock.c tinyproxy.h ../config.h dnscache.h log.h sock.h utils.h \
	conns.h
stats.o: stats.c tinyproxy.h ../config.h log.h stats.h conns.h utils.h
ternary.o: ternary.c tinyproxy.h ../config.h log.h ternary.h utils.h \
	conns.h
thread.o: thread.c tinyproxy.h ../config.h log.h reqs.h sock.h thread.h \
	utils.h conns.h
tinyproxy.o: tinyproxy.c tinyproxy.h ../config.h anonymous.h ternary.h \
	buffer.h dnscache.h filter.h log.h reqs.h sock.h stats.h \
	conns.h thread.h utils.h
uri.o: uri.c tinyproxy.h ../config.h log.h regexp.h uri.h utils.h
utils.o: utils.c tinyproxy.h ../config.h buffer.h conns.h log.h sock.h \
	utils.h

info-am:
info: info-am
dvi-am:
dvi: dvi-am
check-am: all-am
check: check-am
installcheck-am:
installcheck: installcheck-am
install-exec-am: install-binPROGRAMS
install-exec: install-exec-am

install-data-am:
install-data: install-data-am

install-am: all-am
	@$(MAKE) $(AM_MAKEFLAGS) install-exec-am install-data-am
install: install-am
uninstall-am: uninstall-binPROGRAMS
uninstall: uninstall-am
all-am: Makefile $(PROGRAMS)
all-redirect: all-am
install-strip:
	$(MAKE) $(AM_MAKEFLAGS) AM_INSTALL_PROGRAM_FLAGS=-s install
installdirs:
	$(mkinstalldirs)  $(DESTDIR)$(bindir)


mostlyclean-generic:

clean-generic:

distclean-generic:
	-rm -f Makefile $(CONFIG_CLEAN_FILES)
	-rm -f config.cache config.log stamp-h stamp-h[0-9]*

maintainer-clean-generic:
	-test -z "scannerlgrammarhgrammarc" || rm -f scannerl grammarh grammarc
mostlyclean-am:  mostlyclean-binPROGRAMS mostlyclean-compile \
		mostlyclean-tags mostlyclean-generic

mostlyclean: mostlyclean-am

clean-am:  clean-binPROGRAMS clean-compile clean-tags clean-generic \
		mostlyclean-am

clean: clean-am

distclean-am:  distclean-binPROGRAMS distclean-compile distclean-tags \
		distclean-generic clean-am

distclean: distclean-am

maintainer-clean-am:  maintainer-clean-binPROGRAMS \
		maintainer-clean-compile maintainer-clean-tags \
		maintainer-clean-generic distclean-am
	@echo "This command is intended for maintainers to use;"
	@echo "it deletes files that may require special tools to rebuild."

maintainer-clean: maintainer-clean-am

.PHONY: mostlyclean-binPROGRAMS distclean-binPROGRAMS clean-binPROGRAMS \
maintainer-clean-binPROGRAMS uninstall-binPROGRAMS install-binPROGRAMS \
mostlyclean-compile distclean-compile clean-compile \
maintainer-clean-compile tags mostlyclean-tags distclean-tags \
clean-tags maintainer-clean-tags distdir info-am info dvi-am dvi check \
check-am installcheck-am installcheck install-exec-am install-exec \
install-data-am install-data install-am install uninstall-am uninstall \
all-redirect all-am all installdirs mostlyclean-generic \
distclean-generic clean-generic maintainer-clean-generic clean \
mostlyclean distclean maintainer-clean


#scanner.c: scanner.l grammar.h
#	$(LEX) $(AM_LFLAGS) $(LFLAGS) -i $< && mv $(LEX_OUTPUT_ROOT).c $@

clean:
	rm -f *.da
	rm -f gmon.out

# Tell versions [3.59,3.63) of GNU make to not export all variables.
# Otherwise a system limit (for SysV at least) may be exceeded.
.NOEXPORT:
