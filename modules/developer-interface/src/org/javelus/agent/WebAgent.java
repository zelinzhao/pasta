/*
 * Copyright (C) 2012  Tianxiao Gu. All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Please contact Institute of Computer Software, Nanjing University, 
 * 163 Xianlin Avenue, Nanjing, Jiangsu Provience, 210046, China,
 * or visit moon.nju.edu.cn if you need additional information or have any
 * questions.
 */
package org.javelus.agent;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;

public class WebAgent extends Thread {

    static int DEFAULT_PORT = 50000;

    private WebAgent() {
        this(DEFAULT_PORT);
    }

    private WebAgent(int port) {
        super("DSU Web Interface Server Thread");
        try {
            serverSocket = new ServerSocket(port);
            setDaemon(true);
            setPriority(MIN_PRIORITY);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * See javaagent option of JVM
     * 
     * @param agentArgs
     * @param inst
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        int port = DEFAULT_PORT;
        if (agentArgs != null && agentArgs.length() > 0) {
            try {
                port = Integer.parseInt(agentArgs);
            } catch (Exception e) {
                port = DEFAULT_PORT;
                System.err
                        .println("Web Agent: parse port error, use default port.");
            }
        }
        new WebAgent(port).start();
    }

    /**
     * for debuging only..
     * 
     * @param agentArgs
     * @throws InterruptedException
     * @throws MalformedURLException
     */
    public static void main(String[] agentArgs) throws InterruptedException,
            MalformedURLException {

        // new WebAgent().start();
        // while(true){
        // Thread.sleep(1000);
        // }
    }

    private ServerSocket serverSocket;
    private boolean running = true;

    public static String INVOKE_DSU = "/invokeDSU";
    public static String DSU_PATH_PARA = "dsupath";
    public static String DSU_SYNC_PARA = "sync";

    private static java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    // private Socket _socket;

    private static void sendHeader(BufferedOutputStream out, int code,
            String contentType, long contentLength, long lastModified)
            throws IOException {
        out.write(("HTTP/1.0 "
                + code
                + " OK\r\n"
                + "Date: "
                + new Date().toString()
                + "\r\n"
                + "Server: DSU Web Interface Server/1.0\r\n"
                + "Content-Type: "
                + contentType
                + "\r\n"
                + "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n"
                + ((contentLength != -1) ? "Content-Length: " + contentLength
                        + "\r\n" : "") + "Last-modified: "
                + new Date(lastModified).toString() + "\r\n" + "\r\n")
                .getBytes());
    }

    private static void sendError(BufferedOutputStream out, int code,
            String message) throws IOException {
        message = message + "<hr>";// + SimpleWebServer.VERSION;
        sendHeader(out, code, "text/html", message.length(),
                System.currentTimeMillis());
        out.write(message.getBytes());
        out.flush();
        out.close();
    }

    @SuppressWarnings("deprecation")
    public static boolean doInvokeDSU(String path) {
        int index = path.indexOf('?');

        if (index == -1) {
            return false;
        }
        path = path.substring(index + 1);
        String[] args = path.split("&");
        if (args.length > 0) {
            String dsuPath = null;
            boolean sync = false;
            for (String arg : args) {
                if (arg.startsWith(DSU_PATH_PARA)) {
                    int i = arg.indexOf("=");
                    if (i == -1) {
                        return false;
                    }
                    dsuPath = arg.substring(i + 1);
                    if (dsuPath.length() == 0) {
                        dsuPath = null;
                    }
                    dsuPath = URLDecoder.decode(dsuPath);
                } else if (arg.startsWith(DSU_SYNC_PARA)) {

                    int i = arg.indexOf("=");
                    if (i == -1) {
                        return false;
                    }
                    try {
                        sync = Boolean.parseBoolean(arg.substring(i + 1));
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            if (dsuPath == null) {
                return false;
            }

            if (sync) {
                System.err.println("Sync invoking started at "
                        + sdf.format(new Date()));
            }
            org.javelus.DeveloperInterface.invokeDSU(dsuPath, sync);
            if (sync) {
                System.err.println("Sync invoking finished at "
                        + sdf.format(new Date()));
            }
            return true;

        }
        return false;
    }

    @Override
    public void run() {

        System.err.println("Web Agent start at " + new Date());

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();

                socket.setSoTimeout(30000);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(
                        socket.getOutputStream());

                try {
                    String request = in.readLine();
                    if (request == null
                            || !request.startsWith("GET ")
                            || !(request.endsWith(" HTTP/1.0") || request
                                    .endsWith("HTTP/1.1"))) {
                        // Invalid request type (no "GET")
                        sendError(out, 500,
                                "Invalid Method. Only support GET now");
                        return;
                    }
                    String path = request.substring(4, request.length() - 9);

                    if (path.startsWith(INVOKE_DSU)) {
                        System.err.format("Web Interface: request [%s].\n",
                                request);
                        boolean invoke = doInvokeDSU(path);
                        if (invoke) {
                            sendHeader(out, 200, "text/html", -1,
                                    System.currentTimeMillis());
                            out.write(("<html><head><title>DSU Invoke DSU"
                                    + "</title></head><body><h3>Invoke successful, Time:"
                                    + sdf.format(new Date()) + " ["
                                    + new Date().getTime() + "]</h3><p></body></html>\n")
                                    .getBytes());
                        } else {
                            sendHeader(out, 200, "text/html", -1,
                                    System.currentTimeMillis());
                            out.write(("<html><head><title>Invalid parameters for invoking DSU"
                                    + "</title></head><body><h3>Invoke Time:"
                                    + sdf.format(new Date()) + "</h3><p></body></html>\n")
                                    .getBytes());
                        }
                    } else {
                        sendHeader(out, 200, "text/html", -1,
                                System.currentTimeMillis());
                        out.write(("<html><head><title>Invoke DSU"
                                + "</title></head><body><h1>Invoke DSU</h1>"
                                + "<form name=\"invoke\" \" method=\"get\" action=\""
                                + INVOKE_DSU
                                + "\">Path: <input type=\"text\" name=\""
                                + DSU_PATH_PARA
                                + "\"/><input type=\"checkbox\" name=\""
                                + DSU_SYNC_PARA
                                + "\"/> sync <input type=\"submit\" value=\"Invoke\" /></form>" + "</body></html>")
                                .getBytes());
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    out.close();
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
