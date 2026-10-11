package com.twekl.dashboard;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.web.SpringServletContainerInitializer;
import com.twekl.dashboard.administration.config.AppInitializer;

import java.io.File;
import java.util.Collections;

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
        tomcat.getConnector();

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
