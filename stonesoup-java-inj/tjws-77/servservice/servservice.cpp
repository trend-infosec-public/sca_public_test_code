// servservice.cpp : Defines the entry point for the console application.
//

// -install "C:\Documents and Settings\Dmitriy.SYSTEMI\My Documents\My Distribution\WebServer
// $Id: servservice.cpp,v 1.3 2004/10/13 07:36:09 drogatkin Exp $
#include "stdafx.h"
#include <stdio.h>
#include <tchar.h>
#include <wtypes.h>
#include <sys/stat.h>

#include <jni.h>
#ifdef _WIN32
#define PATH_SEPARATOR ';'
#else /* UNIX */
#define PATH_SEPARATOR ':'
#endif
#define JVM_DLL "jvm.dll"
#define JAVA_DLL "java.dll"
#define JRE_KEY	    "Software\\JavaSoft\\Java Runtime Environment"
#define JDK_KEY	    "Software\\JavaSoft\\Java Development Kit"

#define SERVICENAME _T("TinyJavaWebServer")
#define MAX_CLASSPATH_LEN 1024
#define MAXPATHLEN 1024
#define SVC_STOP_TIMEOUT 10000  // 10 seconds
#define SVC_START_TIMEOUT 8000 // 8 seconds

#define NUP_SERVICE_NAME   "Mup"
#define TCPIP_SERVICE_NAME "Tcpip"
#define AFD_SERVICE_NAME    "Afd"

#define VERSION _T("1.0")

#define REG_ROOT _T("SOFTWARE\\Rogatkin\\TinyJavaWebServer")
#define REG_K_CURRVER _T("CurrentVersion")
#define REG_V_PATH _T("Path")
#define REG_V_CP _T("CP")


void CALLBACK serviceMain(DWORD dwArgc, LPTSTR *lpszArgv);
void logServiceMessage(LPCTSTR, int);

void stop();
void run();
void cleanup();

BOOL createJVM();
void installService(LPCTSTR serviceName, LPCTSTR displayName, LPCTSTR serviceExe,
                    LPCTSTR dependencies, int currentDependenciesLen,
                    LPCTSTR homeDir, LPCTSTR classPath);
void unistallService(LPCTSTR serviceName);

BOOL fillCP();

jboolean GetApplicationHome(char *buf, jint bufsize);
static jboolean GetPublicJREHome(char *buf, jint bufsize);
static void usage();

// global service variables
BOOL bStandAlone;
SC_HANDLE   		scm;
SERVICE_STATUS_HANDLE   serviceStatusHandle;
SERVICE_STATUS          serviceStatus;       
HANDLE                  threadHandle = NULL;
JavaVM *jvm = NULL;
JNIEnv *env;
jclass jserv_cls;
char *installDir = NULL;
char *customCP = NULL; // class path for servlets


int main(int argc, char* argv[])
{
	SERVICE_TABLE_ENTRY dispatchTable[] = {
        {SERVICENAME, serviceMain },
        {NULL, NULL }
    };
	if(argc > 1) {
		if(_tcsicmp(_T("-run"), argv[1]) == 0) {
			if (argc > 2) {
				installDir = strdup(argv[2]);
			} else
				fillCP();

			if (createJVM()) {
				run();
			}
				cleanup();
		} else if(_tcsicmp(_T("-install"), argv[1]) == 0) { 
			if (argc < 3) {
				usage();
				return -1;
			}
			scm = OpenSCManager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
			installService(argc<5?SERVICENAME:argv[4], argc<6?SERVICENAME:argv[4],
				argc<7?argv[0]:argv[6], NULL, 0, argv[2], argc<4?NULL:argv[3]);
			CloseServiceHandle(scm);
		} else if(_tcsicmp(_T("-uninstall"), argv[1]) == 0) {
			scm = OpenSCManager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
			unistallService(argc>2?argv[2]:SERVICENAME);
			CloseServiceHandle(scm);
		} else if(_tcsicmp(_T("-help"), argv[1]) == 0)
			usage();
	} else {
		if (!StartServiceCtrlDispatcher(dispatchTable)) {
			logServiceMessage(_T("StartServiceCtrlDispatcher failed."), EVENTLOG_ERROR_TYPE);
		}
	}

	return 0;
}

void usage() {
		_tprintf(_T("usage tjwss -install directory [servlet_cp [service_name [display_name [service_exe]]]]]|\n"
		"            -uninstall [service_name]|\n"
		"            -run directory_where_cmdparams_and_[lib]webserver.jar_servlet.jar|\n"
		"            -help\n"
		));
}


jboolean
GetJREPath(char *path, jint pathsize)
{
    char javadll[MAXPATHLEN];
    struct stat s;
	
    if (GetApplicationHome(path, pathsize)) {
		// Is JRE co-located with the application?
		sprintf(javadll, "%s\\bin\\"JAVA_DLL, path);
		if (stat(javadll, &s) == 0) {
			goto found;
		}
		
		// Does this app ship a private JRE in <apphome>\jre directory? 
		sprintf(javadll, "%s\\jre\\bin\\" JAVA_DLL, path);
		if (stat(javadll, &s) == 0) {
			strcat(path, "\\jre");
			goto found;
		}
    }
	
    // Look for a public JRE on this machine.
    if (GetPublicJREHome(path, pathsize)) {
		goto found;
    }
	
    return JNI_FALSE;
	
found:
    return JNI_TRUE;
}

jboolean
GetJVMPath(const char *jrepath, const char *jvmtype,
		   char *jvmpath, jint jvmpathsize)
{
    struct stat s;
    sprintf(jvmpath, "%s\\bin\\%s\\" JVM_DLL, jrepath, jvmtype);
    if (stat(jvmpath, &s) == 0) {
		return JNI_TRUE;
    } else {
		return JNI_FALSE;
    }
}

jboolean
GetApplicationHome(char *buf, jint bufsize)
{
    char *cp;
    GetModuleFileName(0, buf, bufsize);
    *strrchr(buf, '\\') = '\0'; /* remove .exe file name */
    if ((cp = strrchr(buf, '\\')) == 0) {
	/* This happens if the application is in a drive root, and
		* there is no bin directory. */
		buf[0] = '\0';
		return JNI_FALSE;
    }
    *cp = '\0';  /* remove the bin\ part */
    return JNI_TRUE;
}

static jboolean
GetStringFromRegistry(HKEY key, const char *name, char *buf, jint bufsize)
{
    DWORD type, size;
	
    if (RegQueryValueEx(key, name, 0, &type, 0, &size) == 0
		&& type == REG_SZ
		&& (size < (unsigned int)bufsize)) {
		if (RegQueryValueEx(key, name, 0, 0, (unsigned char*)buf, &size) == 0) {
			return JNI_TRUE;
		}
    }
    return JNI_FALSE;
}

static jboolean
GetPublicJREHome(char *buf, jint bufsize)
{
    HKEY key, subkey;
    char version[MAXPATHLEN];
	
    // Find the current version of the JRE
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, JRE_KEY, 0, KEY_READ, &key) != 0) {
		return JNI_FALSE;
    }
	
	// TODO: version can be obtained from start or config parameters
    if (!GetStringFromRegistry(key, "CurrentVersion",
		version, sizeof(version))) {
		RegCloseKey(key);
		return JNI_FALSE;
    }
	
    // Find directory where the current version is installed. 
    if (RegOpenKeyEx(key, version, 0, KEY_READ, &subkey) != 0) {
		RegCloseKey(key);
		return JNI_FALSE;
    }
	
    if (!GetStringFromRegistry(subkey, "JavaHome", buf, bufsize)) {
		RegCloseKey(key);
		RegCloseKey(subkey);
		return JNI_FALSE;
    }
	
	
    RegCloseKey(key);
    RegCloseKey(subkey);
    return JNI_TRUE;
}

void logServiceMessage(LPCTSTR lpcszMsg, int severity=EVENTLOG_INFORMATION_TYPE) {
    TCHAR    chMsg[256];
    HANDLE  evSrc;
    LPCTSTR  lpszStrings[2];
	
    DWORD lastErr = GetLastError();
	
    _stprintf(chMsg, _T("Service error code: %d"), lastErr);
    lpszStrings[0] = chMsg;
    lpszStrings[1] = lpcszMsg;
	
    evSrc = RegisterEventSource(NULL, TEXT(SERVICENAME));
	
    if (evSrc != NULL) {
		ReportEvent(evSrc, severity, 0, 0, NULL, 2, 0,
			(const char**)lpszStrings, NULL);              
		
		(VOID) DeregisterEventSource(evSrc);
    }
	if (bStandAlone)
		_ftprintf(stderr, lpcszMsg);
}

LPTSTR getErrorMsg(DWORD err, LPCTSTR serviceName) {
    LPTSTR msg = 0;
    TCHAR buf[256];
    switch (err) {
	case ERROR_ACCESS_DENIED:
		msg = _T("You are not logged in as the Administrator.");
		break;
	case ERROR_DUP_NAME:
	case ERROR_SERVICE_EXISTS:
		_stprintf(buf, _T("Service has already been added. To remove, run: "
			"tjwss -uninstall [%s]."), serviceName);
		msg = _tcsdup(buf);
		break;
	case ERROR_SERVICE_DOES_NOT_EXIST:
		_stprintf(buf, _T("Service has not been added. To add, run: "
			"tjwss -install [[%s] {service_description}]."), serviceName);
		msg = _tcsdup(buf);
		break;
	case ERROR_INVALID_NAME:
		msg = _T("The service's name is invalid.");
		break;
	case ERROR_INVALID_PARAMETER:
		msg = _T("One of the service's parameters is invalid.");
		break;
	case ERROR_SERVICE_MARKED_FOR_DELETE:
		msg = _T("The service marked for deletion.");
		break;
    }
    return msg;
}

BOOL sendStatusToSCMgr(DWORD dwCurrentState,
                       DWORD dwWin32ExitCode,
                       DWORD dwCheckPoint,
                       DWORD dwWaitHint) {
    BOOL result;
    if (dwCurrentState == SERVICE_START_PENDING)
		serviceStatus.dwControlsAccepted = 0;
    else
		serviceStatus.dwControlsAccepted = SERVICE_ACCEPT_STOP |
		SERVICE_ACCEPT_PAUSE_CONTINUE;
    serviceStatus.dwCurrentState = dwCurrentState;
    serviceStatus.dwWin32ExitCode = dwWin32ExitCode;
    serviceStatus.dwCheckPoint = dwCheckPoint;
    serviceStatus.dwWaitHint = dwWaitHint;
    if (!(result = SetServiceStatus(serviceStatusHandle, &serviceStatus))) {
        logServiceMessage(_T("SetServiceStatus"), EVENTLOG_ERROR_TYPE);
    }
    return result;
}

/* This method dispatches events from the service control manager. */
void CALLBACK serviceCtrl(DWORD dwCtrlCode) {
    DWORD dwState = SERVICE_RUNNING;
    DWORD dwThreadID;
	
    switch(dwCtrlCode) {
	case SERVICE_CONTROL_PAUSE:
		if (serviceStatus.dwCurrentState == SERVICE_RUNNING) {
			SuspendThread(threadHandle);
			dwState = SERVICE_PAUSED;
		}
		break;
	case SERVICE_CONTROL_CONTINUE:
		if (serviceStatus.dwCurrentState == SERVICE_PAUSED) {
			ResumeThread(threadHandle);
			dwState = SERVICE_RUNNING;
		}
		break;
	case SERVICE_CONTROL_STOP:
		dwState = SERVICE_STOP_PENDING;
		sendStatusToSCMgr(SERVICE_STOP_PENDING, NO_ERROR, 1, SVC_STOP_TIMEOUT);
		// Try the shutdown
		CreateThread(NULL,0,
			(LPTHREAD_START_ROUTINE) stop,
			(LPVOID)NULL, 0, &dwThreadID);
		return;
	case SERVICE_CONTROL_INTERROGATE:
		break;
	default:
		break;
    }
    sendStatusToSCMgr(dwState, NO_ERROR, 0, 0);
}

/* This is called when the service control manager starts the service.
* The service stops when this method returns so there is a wait on an
* event at the end of this method.
*/
void CALLBACK serviceMain(DWORD dwArgc, LPTSTR *lpszArgv) {
	if (!fillCP()) {
        cleanup();
		return;
    }

	if (!createJVM()) {
        cleanup();
		return;
    }
	
    serviceStatusHandle = RegisterServiceCtrlHandler(
		TEXT(SERVICENAME),
		(LPHANDLER_FUNCTION)serviceCtrl);
	
    if(!serviceStatusHandle) {
        cleanup();
		return;
    }
	
    serviceStatus.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
    serviceStatus.dwServiceSpecificExitCode = 0;
	
    if(!sendStatusToSCMgr(SERVICE_START_PENDING, NO_ERROR, 1, SVC_START_TIMEOUT)) {
        cleanup();
		return;
    }
    if(!sendStatusToSCMgr(SERVICE_RUNNING, NO_ERROR, 0, 0)) {
        cleanup();
		return;
    }
	run();
    if(serviceStatusHandle != 0) {
        sendStatusToSCMgr(SERVICE_STOPPED, NO_ERROR, 0, 0);
    }
    return;
}

void stop() {
	if (jvm->AttachCurrentThread((LPVOID*)&env, NULL) < 0) {
		logServiceMessage(_T("Thread %d: attach failed\n"));
		return;
	}
	if (jserv_cls) {		
		jmethodID mid;
		mid = env->GetStaticMethodID(jserv_cls, "stop", "()V");
		if (mid == 0) {
			logServiceMessage(_T("Thread %d: Can't find Serv.stop"), EVENTLOG_ERROR_TYPE);
		} else
			env->CallStaticVoidMethod(jserv_cls, mid, NULL);
		
	}
    if (env->ExceptionOccurred()) {
        env->ExceptionDescribe();
    }
    jvm->DetachCurrentThread();
	
    logServiceMessage("stopped");
}

void cleanup() {
	if (jvm)
		jvm->DestroyJavaVM();
	free(installDir);
	free(customCP);
	//FreeLibrary(hLib);
}

void run() {
    jmethodID mid;
    jobjectArray args;

    jserv_cls = env->FindClass("Acme/Serve/Serve");
    if (jserv_cls == 0) {
        fprintf(stderr, "Can't find Acme.Serve.Serve class\n");
        return;
    }
 
    mid = env->GetStaticMethodID(jserv_cls, "main", "([Ljava/lang/String;)V");
    if (mid == 0) {
        fprintf(stderr, "Can't find Acme.Serve.Serve.main(String[])\n");
        return;
    }

    args = env->NewObjectArray(0, 
                        env->FindClass("java/lang/String"), NULL);
    if (args == 0) {
        fprintf(stderr, "Out of memory\n");
        return;
    }
    env->CallStaticVoidMethod(jserv_cls, mid, args);
}

BOOL createJVM() {
    JavaVMInitArgs vm_args;
    jint res;
    char classpath[MAX_CLASSPATH_LEN];
	char userdir[MAXPATHLEN];
	char jvmpath[MAXPATHLEN];
	char jrepath[MAXPATHLEN];
    HINSTANCE handle;
	if (installDir == NULL || strlen(installDir) > MAXPATHLEN)
		return FALSE;
	if (GetJREPath(jrepath, sizeof jrepath) == FALSE) {
		logServiceMessage(_T("Can't find Java VM path"), EVENTLOG_ERROR_TYPE);
		return FALSE;
	}
	if (GetJVMPath(jrepath, "client", jvmpath, sizeof jvmpath) == FALSE) {
		logServiceMessage(_T("Can't find Java VM path"), EVENTLOG_ERROR_TYPE);
		return FALSE;
	}
    // Load the Java VM DLL 
    if ((handle = LoadLibrary(jvmpath)) == 0) {
		logServiceMessage(_T("Error loading JVM"), EVENTLOG_ERROR_TYPE);
		return JNI_FALSE;
    }
	
    // IMPORTANT: specify vm_args version # if you use JDK1.1.2 and beyond 
	FARPROC jni_create_java_vm = GetProcAddress(handle, "JNI_CreateJavaVM");
	FARPROC jni_get_default_java_vm_init_args = GetProcAddress(handle, "JNI_GetDefaultJavaVMInitArgs");
	if(jni_create_java_vm && jni_get_default_java_vm_init_args == NULL) {
		return JNI_FALSE;
	}
	JavaVMOption options[2];
	vm_args.version = JNI_VERSION_1_4;
	vm_args.nOptions = 2;
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_TRUE;
    // Append USER_CLASSPATH to the end of default system class path 
	if (customCP)
		sprintf(classpath, "-Djava.class.path=%s\\webserver.jar%c%s\\lib\\webserver.jar%c"
		"%s\\servlet.jar%c%s\\lib\\servlet.jar%c%s", installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR,
		installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR, customCP);
	else
		sprintf(classpath, "-Djava.class.path=%s\\webserver.jar%c%s\\lib\\webserver.jar%c"
		"%s\\servlet.jar%c%s\\lib\\servlet.jar", installDir, PATH_SEPARATOR, installDir, PATH_SEPARATOR,
		installDir, PATH_SEPARATOR, installDir);
	//printf("class path %s\n", classpath);
	options[0].optionString = classpath;
	sprintf(userdir, "-Duser.dir=%s", installDir);
	options[1].optionString = userdir;
    // Create the Java VM 
    res = ((jint (JNICALL *)(JavaVM **, void **, void *))jni_create_java_vm)(&jvm,(LPVOID*)&env,&vm_args);
    if (res < 0) {
		logServiceMessage(_T("Can't create Java VM"), EVENTLOG_ERROR_TYPE);
        return FALSE;
    }
	return TRUE;
}

BOOL fillCP() {
	HKEY hKey;
	if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, REG_ROOT, 0, KEY_READ, &hKey) != 0) {
		return JNI_FALSE;
    }
	unsigned long type = REG_SZ;
	TCHAR version[10];
	unsigned long size = sizeof version;
	if (RegQueryValueEx(hKey, REG_K_CURRVER, 0, &type, (unsigned char*)version, &size) == 0) {
		HKEY hKeyVer;
		if (RegOpenKeyEx(hKey, version, 0, KEY_READ, &hKeyVer) == 0) {
			size = (sizeof TCHAR)*(MAXPATHLEN+1);
			installDir = (LPTSTR)malloc(size);
			if (RegQueryValueEx(hKeyVer, REG_V_PATH, 0, &type, (unsigned char*)installDir, &size) == 0) {
				size = (sizeof TCHAR)*(MAXPATHLEN+1);
				customCP  = (LPTSTR)malloc(size);
				type = REG_SZ;
				if (RegQueryValueEx(hKeyVer, REG_V_CP, 0, &type, (unsigned char*)customCP, &size) != 0) {
					free(customCP);
					customCP = NULL;
				}
				RegCloseKey(hKeyVer);
				RegCloseKey(hKey);
				return JNI_TRUE;
			}
			RegCloseKey(hKeyVer);
		}
    }
	RegCloseKey(hKey);
	return JNI_FALSE;
}

/* Stop the running NT service */
void endService(char* msg) {
    logServiceMessage(msg);
	
    if (serviceStatusHandle != 0) {
		sendStatusToSCMgr(SERVICE_STOP_PENDING, GetLastError(), 0, 0);
    }
}


void unistallService(LPCTSTR serviceName) {
    
    SC_HANDLE hService = OpenService(scm,
		serviceName,
		SERVICE_ALL_ACCESS);
    
    if (hService == NULL) {
		DWORD err = GetLastError();
		char * msg = getErrorMsg(err, serviceName);
		if (msg == 0) {
			fprintf(stderr, 
				"Cannot open service %s: unrecognized error 0x%02x\n", 
				serviceName, err);
		} else {
			fprintf(stderr, "Cannot open service %s: %s\n", serviceName, msg);
		}
		return;
    }
	
    if((DeleteService(hService))) {
		printf("Removed service %s.\n", serviceName);
    } else {
		DWORD err = GetLastError();
		char *msg = getErrorMsg(err, serviceName);
		if (msg == 0) {
			fprintf(stderr, 
				"Cannot remove service %s: unrecognized error %dL\n", 
				serviceName, err);
		} else {
			fprintf(stderr, "Cannot remove service %s: %s\n", 
				serviceName, msg);
		}
    }
}


/* This is called when the service is installed */
void installService(LPCTSTR serviceName, LPCTSTR displayName, LPCTSTR serviceExe,
                    LPCTSTR dependencies, int currentDependenciesLen,
                    LPCTSTR homeDir, LPCTSTR classPath)  {
    LPCTSTR lpszBinaryPathName = serviceExe;
	char * allDependencies = new TCHAR[200];
    char * ptr = allDependencies;
	if (currentDependenciesLen > 0 && dependencies != NULL) {
		strcpy(ptr, dependencies);
		ptr += currentDependenciesLen;
	}
	
    // add static dependencies
    strcpy(ptr, NUP_SERVICE_NAME);
    ptr += sizeof(NUP_SERVICE_NAME);
    strcpy(ptr, TCPIP_SERVICE_NAME);
    ptr += sizeof(TCPIP_SERVICE_NAME);
    strcpy(ptr, AFD_SERVICE_NAME);
    ptr += sizeof(AFD_SERVICE_NAME);
    
    *ptr = '\0';
	BOOL needToFree = FALSE;
	if (strchr(lpszBinaryPathName, ' ') != NULL || TRUE) {
		char *quotedBinPath = new char[strlen(lpszBinaryPathName)+3];
		sprintf(quotedBinPath, "\"%s\"", lpszBinaryPathName);
		lpszBinaryPathName = quotedBinPath;
		needToFree = TRUE;
	}
	printf("Service %s.\n", lpszBinaryPathName);
    SC_HANDLE hService = CreateService(scm,                          // SCManager database
		serviceName,                 // name of service
		displayName,                 // name to display
		SERVICE_ALL_ACCESS,          // desired access
		SERVICE_WIN32_OWN_PROCESS,   // service type
		SERVICE_AUTO_START,          // start type
		SERVICE_ERROR_NORMAL,        // error control type
		lpszBinaryPathName,          // binary name
		NULL,                        // no load ordering group
		NULL,                        // no tag idenitifier
		allDependencies,                // we depend ???
		NULL,                        // loacalsystem account
		NULL);                       // no password
	delete allDependencies;
	if (needToFree)
		delete (void*)lpszBinaryPathName;
    if (hService != NULL) {
		printf("Added service %s.\n", serviceName);
		HKEY hKey;	
		if(RegCreateKeyEx(HKEY_LOCAL_MACHINE,
			REG_ROOT,
			0,
			NULL,
			REG_OPTION_NON_VOLATILE,
			KEY_ALL_ACCESS,
			NULL,
			&hKey, NULL) == ERROR_SUCCESS) {
			if (RegSetValueEx(
				hKey,
				REG_K_CURRVER,
				0,
				REG_SZ,
				(const BYTE* )VERSION,
				sizeof VERSION 
				) == ERROR_SUCCESS) {
				HKEY hKeyVer;	
				if(RegCreateKeyEx(hKey,
					VERSION,
					0,
					NULL,
					REG_OPTION_NON_VOLATILE,
					KEY_ALL_ACCESS,
					NULL,
					&hKeyVer, NULL) == ERROR_SUCCESS) {
					if (RegSetValueEx(
						hKeyVer,
						REG_V_PATH,
						0,
						REG_SZ,
						(const BYTE* )homeDir,
						_tcslen(homeDir)+sizeof TCHAR 
						) == ERROR_SUCCESS) {
							printf("Set path %s.\n", homeDir);	
					}
					if (classPath) {
						if (RegSetValueEx(
							hKeyVer,
							REG_V_CP,
							0,
							REG_SZ,
							(const BYTE* )classPath,
							_tcslen(classPath)+sizeof TCHAR 
							) == ERROR_SUCCESS) {
							printf("Set class path %s.\n", classPath);	
						}
					}
					RegCloseKey(hKeyVer);
				}
				RegCloseKey(hKey);
			}
		} else {
			fprintf(stderr, "Cannot create config info in the Registry.");
		}

    } else {
		DWORD err = GetLastError();
		char *msg = getErrorMsg(err, serviceName);
		if (msg == 0) {
			fprintf(stderr, 
				"Cannot create service %s: unrecognized error %dL\n", 
				serviceName, err);
		} else {
			fprintf(stderr, "Cannot create service %s: %s\n", 
				serviceName, msg);
		}
		return;
    }
    CloseServiceHandle(hService);
}
