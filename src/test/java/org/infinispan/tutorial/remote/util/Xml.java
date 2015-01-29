package org.infinispan.tutorial.remote.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Xml {

   public static void addInfinispanDependency(String serverDir, String dependencyName) {
      // Back up original module.xml in order to restore it in the cleanup phase
      FileSystem fs = FileSystems.getDefault();
      String base = "/modules/system/layers/base/org/infinispan";
      String moduleXmlRelativePath = base + "/tutorial/main/module.xml";
      Path moduleXmlPath = fs.getPath(serverDir, moduleXmlRelativePath);
      Path moduleXmlBackupPath = fs.getPath(serverDir, moduleXmlRelativePath + ".bak");
      try {
         Files.copy(moduleXmlPath, moduleXmlBackupPath, StandardCopyOption.REPLACE_EXISTING);

         // Read main Infinispan module.xml and add dependency parameter
         DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
         DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
         String commonsModuleXmlPath = base + "/commons/main/module.xml";
         File commonsModuleXmlFile = new File(serverDir, commonsModuleXmlPath);
         Document doc = docBuilder.parse(commonsModuleXmlFile);
         Node dependencies = doc.getElementsByTagName("dependencies").item(0);
         Element newElement = doc.createElement("module");
         newElement.setAttribute("name", dependencyName);
         dependencies.appendChild(newElement);

         // Save the main Infinispan module.xml file
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         Transformer transformer = transformerFactory.newTransformer();
         DOMSource source = new DOMSource(doc);
         StreamResult result = new StreamResult(commonsModuleXmlFile);
         transformer.transform(source, result);
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   public static void removeInfinispanDependency(String serverDir) {
      // Removing the Infinispan dependency is just a matter of restoring
      // the originally backed up version of the module.xml
      FileSystem fs = FileSystems.getDefault();
      String moduleXmlRelativePath = "/modules/system/layers/base/org/infinispan/tutorial/main/module.xml";
      Path moduleXmlPath = fs.getPath(serverDir, moduleXmlRelativePath);
      Path moduleXmlBackupPath = fs.getPath(serverDir, moduleXmlRelativePath + ".bak");
      try {
         Files.copy(moduleXmlBackupPath, moduleXmlPath, StandardCopyOption.REPLACE_EXISTING);
         Files.delete(moduleXmlBackupPath);
      } catch (IOException e) {
         throw new AssertionError(e);
      }
   }

}
