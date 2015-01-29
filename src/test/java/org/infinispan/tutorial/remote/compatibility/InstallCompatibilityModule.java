package org.infinispan.tutorial.remote.compatibility;

import org.infinispan.tutorial.remote.util.Xml;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * An Arquillian container lifecycle listener that allows common, shared,
 * classes for compatibility mode to be installed before the server starts,
 * and be cleared up when server stops.
 */
public class InstallCompatibilityModule implements LoadableExtension {

   @Override
   public void register(ExtensionBuilder extensionBuilder) {
      // Register a listener for in-container events
      extensionBuilder.observer(CompatibilityModule.class);
   }

   public static class CompatibilityModule {

      @SuppressWarnings("unused")
      public void install(@Observes BeforeStart event) throws IOException {
         // Module install phase before server starts, check this is a compatibility test
         if (System.getProperty("arquillian.launch").equals("tutorial-compatibility")) {
            // Retrieve the location of the server distribution using system properties
            String serverDir = System.getProperty("server1.dist");
            System.out.printf("Create shared class module in %s %n", serverDir);

            // Create all intermediate directories for compatibility shared classes module
            FileSystem fs = FileSystems.getDefault();
            String mainRelativePath = "/modules/system/layers/base/org/infinispan/tutorial/main";
            Path mainPath = fs.getPath(serverDir, mainRelativePath);
            Files.createDirectories(mainPath);

            // Write the module.xml descriptor for the module
            String moduleXml = getModuleXml();
            Path moduleXmlPath = fs.getPath(serverDir, mainRelativePath + "/module.xml");
            Files.write(moduleXmlPath, moduleXml.getBytes(Charset.forName("UTF-8")), StandardOpenOption.CREATE);

            // Write the jar containing the classes for the module
            InputStream jar = createStockValueJar();
            Path jarPath = fs.getPath(serverDir, mainRelativePath + "/compatibility-stock-value.jar");
            Files.copy(jar, jarPath, StandardCopyOption.REPLACE_EXISTING);

            // Finally, add the compatibility module as dependency for Infinispan
            // so that classes can be found by the classloading logic
            Xml.addInfinispanDependency(serverDir, "org.infinispan.tutorial");
         }
      }

      private String getModuleXml() {
         return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<module xmlns=\"urn:jboss:module:1.3\" name=\"org.infinispan.tutorial\">\n" +
               "   <resources>\n" +
               "      <resource-root path=\"compatibility-stock-value.jar\"/>\n" +
               "   </resources>\n" +
               "   \n" +
               "   <dependencies/>\n" +
               "</module>\n";
      }

      private static InputStream createStockValueJar() {
         // Use Shrinkwrap to create a jar file containing the desired classes
         return ShrinkWrap.create(JavaArchive.class, "compatibility-stock-value.jar")
               .addClasses(StockValue.class).as(ZipExporter.class).exportAsInputStream();
      }

      @SuppressWarnings("unused")
      public void uninstall(@Observes AfterStop event) {
         // Module uninstall phase after server has stopped, check this is a compatibility test
         if (System.getProperty("arquillian.launch").equals("tutorial-compatibility")) {
            // Retrieve the location of the server distribution using system properties
            String serverDir = System.getProperty("server1.dist");
            System.out.printf("Delete shared class module from %s %n", serverDir);

            // Remove compatibility module dependency from main Infinispan module
            Xml.removeInfinispanDependency(serverDir);

            // Delete all module files
            Path path = Paths.get(serverDir, "/modules/system/layers/base/org/infinispan/tutorial/main/");
            try {
               Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                     Files.delete(file);
                     return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                     Files.delete(file);
                     return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                     if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                     }

                     throw exc;
                  }
               });
            } catch (IOException e) {
               throw new AssertionError(e);
            }
         }
      }
   }

}
