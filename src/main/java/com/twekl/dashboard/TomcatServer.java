package com.twekl.dashboard;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * Pure Apache Tomcat 10.1 Standalone Runner.
 * Zero Spring Boot dependencies.
 */
public class TomcatServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {}
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        var connector = tomcat.getConnector();
        connector.setProperty("maxThreads", System.getenv().getOrDefault("HTTP_MAX_THREADS", "150"));
        connector.setProperty("minSpareThreads", System.getenv().getOrDefault("HTTP_MIN_SPARE_THREADS", "16"));
        connector.setProperty("maxConnections", System.getenv().getOrDefault("HTTP_MAX_CONNECTIONS", "1024"));
        connector.setProperty("acceptCount", System.getenv().getOrDefault("HTTP_ACCEPT_COUNT", "200"));
        connector.setProperty("connectionTimeout", "20000");
        connector.setProperty("compression", "on");
        connector.setProperty("compressionMinSize", "256");
        connector.setProperty("compressableMimeType", "text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json");

        String baseDir = new File("target/tomcat-work").getAbsolutePath();
        new File(baseDir).mkdirs();
        tomcat.setBaseDir(baseDir);

        String docBase = new File("src/main/webapp").getAbsolutePath();
        new File(docBase).mkdirs();
        Context context = tomcat.addWebapp("", docBase);
        context.setParentClassLoader(TomcatServer.class.getClassLoader());

        File additionWebInfClasses = new File("target/classes");
        if (additionWebInfClasses.exists()) {
            StandardRoot resources = new StandardRoot(context);
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    additionWebInfClasses.getAbsolutePath(), "/"));
            context.setResources(resources);
        }


        System.out.println("=================================================");
        System.out.println(" Starting Apache Tomcat 10.1 Engine on port " + port);
        System.out.println(" Pure Spring MVC Framework (Zero Spring Boot)");
        System.out.println("=================================================");

        tomcat.start();
        tomcat.getServer().await();
    }
}
