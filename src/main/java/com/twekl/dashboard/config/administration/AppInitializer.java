package com.twekl.dashboard.config.administration;

import jakarta.servlet.Filter;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Pure Spring MVC Web Application Initializer for Apache Tomcat. Replaces Spring Boot with standard
 * Servlet 6 DispatcherServlet bootstrap.
 */
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected void registerContextLoaderListener(jakarta.servlet.ServletContext servletContext) {
        super.registerContextLoaderListener(servletContext);
        servletContext.addListener(
                new org.springframework.web.context.request.RequestContextListener());
    }

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] {JpaConfig.class, SecurityConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {WebMvcConfig.class};
    }

    @Override
    protected void customizeRegistration(jakarta.servlet.ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(
                new jakarta.servlet.MultipartConfigElement("", 1048576, 1200000, 0));
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {"/"};
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[] {
            encodingFilter, new DelegatingFilterProxy("springSecurityFilterChain")
        };
    }
}
