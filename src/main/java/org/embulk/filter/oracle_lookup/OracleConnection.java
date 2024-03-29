package org.embulk.filter.oracle_lookup;

import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Paths;
import java.util.Optional;

import org.embulk.config.ConfigException;
import org.slf4j.Logger;
import java.util.concurrent.atomic.AtomicReference;
import java.net.URLClassLoader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OracleConnection {

    private static Connection connection=null;

    private OracleConnection(OracleLookupFilterPlugin.PluginTask task) throws Exception {
        try{
            if(task.getDriverClass().isPresent() && task.getDriverPath().isPresent()){
                this.loadOracleJdbcDriver(task.getDriverClass().get(),task.getDriverPath());
            }else{
                this.loadOracleJdbcDriver("oracle.jdbc.driver.OracleDriver",task.getDriverPath());
            }
            String url;
            if(task.getURL().isPresent()){
                url= task.getURL().get();
            }else if(task.getSID().isPresent() && task.getHost().isPresent() && task.getPort().isPresent()){
                url="jdbc:oracle:thin:@" + task.getHost().get() + ":"+task.getPort().get()+":"+task.getSID().get();
            }else{
                throw new RuntimeException("sid_name,host and port no must be provided by the user if url has not been provided");
            }
            connection= DriverManager.getConnection(url, task.getUserName(), task.getPassword());
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    public static Connection getConnection(OracleLookupFilterPlugin.PluginTask task){
        try {
            if(connection==null || connection.isClosed()){
                try {
                    new OracleConnection(task);
                    return connection;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return connection;
    }
    private Class<? extends java.sql.Driver> loadOracleJdbcDriver(
            final String className,
            final Optional<String> driverPath)
    {
        synchronized (oracleJdbcDriver) {
            if (oracleJdbcDriver.get() != null) {
                return oracleJdbcDriver.get();
            }

            if (driverPath.isPresent()) {
                logger.info(
                        "\"driver_path\" is set to load the Oracle JDBC driver class \"{}\". Adding it to classpath.", className);
                this.addDriverJarToClasspath(driverPath.get());
            }

            try {
                // If the class is found from the ClassLoader of the plugin, that is prioritized the highest.
                final Class<? extends java.sql.Driver> found = loadOracleJdbcDriverClassForName(className);
                oracleJdbcDriver.compareAndSet(null, found);

                if (driverPath.isPresent()) {
                    logger.warn(
                            "\"driver_path\" is set while the Oracle JDBC driver class \"{}\" is found from the PluginClassLoader."
                                    + " \"driver_path\" is ignored.", className);
                }
                return found;
            }
            catch (final ClassNotFoundException ex) {
                throw new ConfigException("The Oracle JDBC driver for the class \"" + className + "\" is not found.", ex);
            }

        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends java.sql.Driver> loadOracleJdbcDriverClassForName(final String className) throws ClassNotFoundException
    {
        return (Class<? extends java.sql.Driver>) Class.forName(className);
    }

    private static final AtomicReference<Class<? extends java.sql.Driver>> oracleJdbcDriver = new AtomicReference<>();

    private static final Logger logger = LoggerFactory.getLogger(OracleConnection.class);

    protected void addDriverJarToClasspath(String glob)
    {
        // TODO match glob
        final ClassLoader loader = getClass().getClassLoader();
        if (!(loader instanceof URLClassLoader)) {
            throw new RuntimeException("Plugin is not loaded by URLClassLoader unexpectedly.");
        }
        if (!"org.embulk.plugin.PluginClassLoader".equals(loader.getClass().getName())) {
            throw new RuntimeException("Plugin is not loaded by PluginClassLoader unexpectedly.");
        }
        Path path = Paths.get(glob);
        if (!path.toFile().exists()) {
            throw new ConfigException("The specified driver jar doesn't exist: " + glob);
        }
        final Method addPathMethod;
        try {
            addPathMethod = loader.getClass().getMethod("addPath", Path.class);
        } catch (final NoSuchMethodException ex) {
            throw new RuntimeException("Plugin is not loaded a ClassLoader which has addPath(Path), unexpectedly.");
        }
        try {
            addPathMethod.invoke(loader, Paths.get(glob));
        } catch (final IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof MalformedURLException) {
                throw new IllegalArgumentException(targetException);
            } else if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        }
    }

    protected File findPluginRoot()
    {
        try {
            URL url = getClass().getResource("/" + getClass().getName().replace('.', '/') + ".class");
            if (url.toString().startsWith("jar:")) {
                url = new URL(url.toString().replaceAll("^jar:", "").replaceAll("![^!]*$", ""));
            }

            File folder = new File(url.toURI()).getParentFile();
            for (;; folder = folder.getParentFile()) {
                if (folder == null) {
                    throw new RuntimeException("Cannot find 'embulk-filter-xxx' folder.");
                }

                if (folder.getName().startsWith("embulk-input-")) {
                    return folder;
                }
            }
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
