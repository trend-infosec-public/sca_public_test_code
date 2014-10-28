
static char data_rights_legend [ ] = 
  "This software (or technical data) was produced for the U. S. \
   Government under contract 2009-0917826-016 and is subject to \
   the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007). \
\
   (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.";




/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/***********************************************************************
**
**
**  
**  Date: 22 Mar 12
**
**  Revision History
**  Date      Change
**  22 Mar 12 Initial 
**
** This program is a TCP socket server which does matrix multiplication.  It 
** opens a TCP server socket on a port you specify.  It waits for one client 
** to connect, shuts down the server socket, services that client, and then 
** re-opens the server socket.
**
** Clients send the matrices to be multiplied as a plain text 
** whitespace-delimited sequence of integers.  A matrix is represented as:
**   [rowcount] [columncount] [val] [val] ...
**
** Error: If the number of rows, columns, and values in the second matrix
** are not in sync, the matrix data structure is left in an invalid state:
** NULL pointers may be left in the tail end of the 'rows' field.  This will
** cause a NULL pointer dereference and crash when the matrices are multiplied.
**
** Note: If the last row of the second matrix is partially present, the 
** remaining missing values are initialized to zero and there will be no crash.
** To crash it, the first matrix must be complete, and there must be rows in 
** the second matrix for which no values at all exist in the integer stream.
**
** STONESOUP Weakness Class: NULL Pointer Errors
** CWE ID: CWE-476
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: 986
**
** Variant Features:
**
**
** I/0 Pairs:
**   Good: 1st Set:somefile1.txt
**         2nd Set:somefile2.txt
**         3rd Set:somefile3.txt
**         4th Set:
**         5th Set:
**    Bad: 1st Set: somefile4:txt - Linux
**             2nd Set: somefile5&txt - Windows
**
** How program works:
**     Run as [app.exe] [server port number]
************************************************************************/

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#    include <winsock2.h>
#else
#    include <sys/socket.h>
#    include <netinet/in.h>
#    include <unistd.h>
#endif

/* try to write code usable both with winsock and posix sockets */
#ifndef _WIN32
typedef int SOCKET;
typedef int BOOL;
#    define SOCKET_ERROR -1
#    define TRUE 1
#    define INVALID_SOCKET -1
#    define SD_SEND SHUT_WR
#    define WSAGetLastError() errno
#    define closesocket(x) close(x)
#    define Sleep(x) sleep((x)/1000)
#endif

/**
 * A matrix structure.  Values are stored as an array of pointers
 * to arrays of ints (the rows).
 */
struct Matrix
{
    int width, height;
    int **rows;
};

/** define to larger than the max number of characters in an int. */
#define MAXINTWIDTH 50

/**
 * Frees the memory allocated for the given matrix.  Partially constructed
 * (and non-constructed, i.e. NULL) matrices are ok as long as pointers are
 * NULL'd out; only allocated memory will be freed.
 */
void freeMatrix(struct Matrix *mat)
{
    int i;

    if (mat)
    {
        if (mat->rows)
        {
            for (i = 0; i < mat->height; ++i)
                if (mat->rows[i])
                    free(mat->rows[i]);
            free(mat->rows);
        }
        free(mat);
    }
}

/**
 * Reads another int from the given socket.  If "quit" is received, the
 * application immediately terminates.
 * \return EOF if the end of stream is reached, a positive errno value on
 *   other errors, or zero on success.
 */
int nextInt(SOCKET sd, int *result)
{
    char ch;
    int bytesRecvd;
    char numBuf[MAXINTWIDTH];
    int readIdx;
    
    /* first skip over whitespace... */
    do
    {
        bytesRecvd = recv(sd, &ch, 1, 0);
    }
    while(bytesRecvd == 1 && isspace(ch));
    
    if (bytesRecvd == SOCKET_ERROR)
        return WSAGetLastError();

    if (bytesRecvd == 0)
    {
        fprintf(stderr, "No bytes received.\n");
        return EOF;
    }

    numBuf[0] = ch;
    readIdx = 1;

    /* then read the next value */
    do
    {
        bytesRecvd = recv(sd, &ch, 1, 0);
        /*
          // STONESOUP:INTERACTION_POINT
         */
        if (bytesRecvd == 1 && !isspace(ch))
            numBuf[readIdx++] = ch;
    }
    while(bytesRecvd == 1 && !isspace(ch) && readIdx < MAXINTWIDTH);
    
    if (bytesRecvd == SOCKET_ERROR)
        return WSAGetLastError();

    if (readIdx >= MAXINTWIDTH)
    {
        fprintf(stderr, "Integer had too many digits!\n");
        return ERANGE;
    }

    numBuf[readIdx] = 0; /* null-terminate */

    /* a quick hack... terminate if "quit" is received... */
    if (!strncmp(numBuf, "quit", sizeof(numBuf)))
    {
        fprintf(stdout, "Received quit command.\n");
        fflush(stdout);
        exit(0);
    }

    if (sscanf(numBuf, "%d", result) != 1)
    {
        fprintf(stderr, "Invalid integer: %s\n", numBuf);
        return EINVAL;
    }

    fprintf(stdout, "Received integer: %s\n", numBuf);
    fflush(stdout);

    return 0;
}

/**
 * Creates a matrix by reading values from the given socket.
 * \return NULL if bad input was received and the matrix could not be created.
 */
struct Matrix *readMatrix(SOCKET sd)
{
    int width, height, err;
    int i, j;
    struct Matrix *mat = NULL;
    int tmp;
    
    mat = (struct Matrix *)calloc(1, sizeof(struct Matrix));
    if (!mat)
    {
        perror("calloc");
        goto error_exit;
    }
    
    if (nextInt(sd, &height) != 0)
    {
        fprintf(stderr, "Couldn't read matrix height\n");
        goto error_exit;
    }
    
    if (nextInt(sd, &width) != 0)
    {
        fprintf(stderr ,"Couldn't read matrix width\n");
        goto error_exit;
    }
    
    if (width <= 0)
    {
        fprintf(stderr, "Illegal nonpositive width (%d)\n", width);
        goto error_exit;
    }
    
    if (height <= 0)
    {
        fprintf(stderr, "Illegal nonpositive height (%d)\n", height);
        goto error_exit;
    }
    
    mat->width = width;
    mat->height = height;

    mat->rows = (int **)calloc(mat->height, sizeof(int *));
    if (!mat->rows)
    {
        perror("calloc");
        goto error_exit;
    }

    /* read the matrix values, allocating rows as we go */
    i = j = err = 0;
    while (i < mat->height)
    {
        err = nextInt(sd, &tmp);
        if (err != 0)
            break;

        if (j == 0)
        {
            /* starting a new row; allocate memory for it */
            mat->rows[i] = (int *)calloc(mat->width, sizeof(int));
            if (!mat->rows[i])
            {
                perror("calloc");
                goto error_exit;
            }
        }

        mat->rows[i][j] = tmp;

        ++j;
        if (j == mat->width)
        {
            ++i;
            j = 0;
        }
    }

    if (err > 0)
    {
        /* nextInt() will have already printed a more descriptive message */
        fprintf(stderr, "Error reading matrix\n");
        goto error_exit;
    }

    return mat;
    /*
      // STONESOUP:CROSSOVER_POINT
    */

error_exit:
    freeMatrix(mat);
    return NULL;
}

/**
 * Returns the product of the two given matrices in a newly allocated matrix.
 * \return NULL if the multiplication could not be performed.
 */
struct Matrix *multiply(struct Matrix *mat1, struct Matrix *mat2)
{
    struct Matrix *prod = NULL;
    int sum;
    int prodi, prodj, k;

    if (mat1->width != mat2->height)
    {
        fprintf(stderr, "Incompatible matrix dimensions! (%dx%d) and (%dx%d)\n",
            mat1->height, mat1->width, mat2->height, mat2->width);
        goto error_exit;
    }
    
    prod = (struct Matrix *)calloc(1, sizeof(struct Matrix));
    if (!prod)
    {
        perror("calloc");
        goto error_exit;
    }
    
    prod->width = mat2->width;
    prod->height = mat1->height;
    prod->rows = (int **)calloc(prod->height, sizeof(int*));
    if (!prod->rows)
    {
        perror("calloc");
        goto error_exit;
    }

    for (prodi = 0; prodi < mat1->height; ++prodi)
    {
        prod->rows[prodi] = (int *)calloc(prod->width, sizeof(int));
        if (!prod->rows[prodi])
        {
            perror("calloc");
            goto error_exit;
        }

        for (prodj = 0; prodj < mat2->width; ++prodj)
        {
            sum = 0;
            for (k = 0; k < mat1->width; ++k)
                sum += mat1->rows[prodi][k] * mat2->rows[k][prodj];
                /*
                  // STONESOUP:TRIGGER_POINT
                 */

            prod->rows[prodi][prodj] = sum;
        }
    }

    return prod;

error_exit:
    freeMatrix(prod);
    return NULL;
}

/**
 * Prints the matrix entries to stdout. (for debugging)
 */
void printMatrix(struct Matrix *mat)
{
    int i, j;
    for (i=0; i<mat->height; ++i)
    {
        for (j=0; j<mat->width; ++j)
            fprintf(stdout, "%d ", mat->rows[i][j]);
        fprintf(stdout, "\n");
        fflush(stdout);
    }
}

/**
 * Sends \p count chars of \p str over the given socket.
 *
 * \return zero on success; a nonzero error code if an error occurred.
 */
int sendString(SOCKET sd, const char *str, int count)
{
    int total, sent;
    int err;

    total = sent = 0;
    while (total < count)
    {
        sent = send(sd, &str[total], count - total, 0);
        if (sent == SOCKET_ERROR)
        {
            err = WSAGetLastError();
            fprintf(stderr, "Error sending data to client: %d\n", err);
            return err;
        }

        total += sent;
    }

    return 0;
}

/**
 * Sends the given matrix in plain text through the given socket.
 * Rows are delimited by newlines, columns by spaces.
 * \return zero on success; a non-zero error code on failure
 */
int sendMatrix(SOCKET sd, struct Matrix *mat)
{
    int i, j;
    char numBuf[MAXINTWIDTH];
    int len;
    int err;

    for (i = 0; i < mat->height; ++i)
    {
        for (j = 0; j < mat->width; ++j)
        {
            /* snprintf() requires -std=c99 or later, so I can't
               use it.  So this isn't as safe as it should be. */
            len = sprintf(numBuf, "%d", mat->rows[i][j]);
            if (len < 0 || len == MAXINTWIDTH)
            {
                fprintf(stderr, "Buffer overflow converting %d to a string!\n",
                    mat->rows[i][j]);
                return ERANGE;
            }

            if ((err = sendString(sd, numBuf, len)))
                return err;
            if ((err = sendString(sd, " ", 1)))
                return err;
        }

        if ((err = sendString(sd, "\n", 1)))
            return err;
    }

    return 0;
}

/**
 * Opens a TCP server socket on the given port, accepts a connection from
 * one client, then shuts down the server socket and returns the client
 * socket.
 * \return zero on success, or a non-zero error code on failure.
 */
int getClientSocket(unsigned short port, SOCKET *sd)
{
    int err = 0;
    struct sockaddr_in serverAddr;
    BOOL tmp;
    SOCKET ssd; /* the server socket */
    
    *sd = ssd = INVALID_SOCKET;

    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(port);
    
    ssd = socket(AF_INET, SOCK_STREAM, 0);
    if (ssd == INVALID_SOCKET)
        return WSAGetLastError();
    
    tmp = TRUE;
    if (setsockopt(ssd, SOL_SOCKET, SO_REUSEADDR, (const char *)&tmp,
            sizeof(tmp)) == SOCKET_ERROR)
    {
        err = WSAGetLastError();
        goto error_exit;
    }
    
    if (bind(ssd, (const struct sockaddr *)&serverAddr, sizeof(serverAddr))
            == SOCKET_ERROR)
    {
        err = WSAGetLastError();
        goto error_exit;
    }
    
    if (listen(ssd, 1) == SOCKET_ERROR)
    {
        err = WSAGetLastError();
        goto error_exit;
    }

    fprintf(stdout, "Listening on port %hu...\n", port);
    fflush(stdout);

    if ((*sd = accept(ssd, NULL, NULL)) == INVALID_SOCKET)
    {
        err = WSAGetLastError();
        goto error_exit;
    }

    fprintf(stdout, "Connection acquired.\n");
    fflush(stdout);

    /* got our connection, now shut down the server socket. */
    if (closesocket(ssd) == SOCKET_ERROR)
    {
        err = WSAGetLastError();
        fprintf(stderr, "error closing server socket: %d\n", err);
        ssd = INVALID_SOCKET; /* so we don't try closing it again */
        goto error_exit;
    }

    return 0;

error_exit:
    if (ssd != INVALID_SOCKET)
        if (closesocket(ssd) == SOCKET_ERROR)
            fprintf(stderr, "error closing server socket: %d\n", WSAGetLastError());

    if (*sd != INVALID_SOCKET)
    {
        if (closesocket(*sd) == SOCKET_ERROR)
            fprintf(stderr, "error closing client socket: %d\n", WSAGetLastError());
        *sd = INVALID_SOCKET;
    }

    return err;
}

int main(int argc, const char *argv[])
{
    struct Matrix *mat1, *mat2, *prod;
    int err;
    unsigned short port;
    SOCKET sd = INVALID_SOCKET;
#ifdef _WIN32
    WSADATA wsaData;
#endif

    if (argc < 2)
    {
        fprintf(stderr, "Usage: %s <port>\n", argv[0]);
        return 1;
    }
    
    if (sscanf(argv[1], "%hu", &port) != 1)
    {
        fprintf(stderr, "Invalid port (%hu)\n", port);
        return 1;
    }

#ifdef _WIN32
    if ((err = WSAStartup(MAKEWORD(2, 2), &wsaData)))
    {
        fprintf(stderr, "Could not initialize winsock: %d\n", err);
        return 1;
    }
#endif

    mat1 = mat2 = prod = NULL;

    /* The server main loop */
    for(;;)
    {
        freeMatrix(mat1);
        freeMatrix(mat2);
        freeMatrix(prod);
        mat1 = mat2 = prod = NULL;

        if (sd != INVALID_SOCKET)
        {
            /* This prevents potential connection-reset errors in clients on
               windows... */
            if (shutdown(sd, SD_SEND) == SOCKET_ERROR)
                fprintf(stderr, "error in TCP send shutdown: %d\n",
                    WSAGetLastError());

            if (closesocket(sd) == SOCKET_ERROR)
                fprintf(stderr, "error closing client socket: %d\n",
                    WSAGetLastError());

            sd = INVALID_SOCKET;
        }

        if ((err = getClientSocket(port, &sd)) != 0)
        {
            fprintf(stderr, "Error acquiring client connection: %d\n",
                err);
            /* in case we are repeatedly unable to get a client connection
               for some reason, don't spam. */
            Sleep(1000);
            continue;
        }

        mat1 = readMatrix(sd);
        if (!mat1)
            continue;

        mat2 = readMatrix(sd);
        if (!mat2)
            continue;

        prod = multiply(mat1, mat2);
        if (!prod)
            continue;

        /* printMatrix(prod); */
        sendMatrix(sd, prod);
    }
}

