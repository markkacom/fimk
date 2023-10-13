/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;


public class NxtProperties {

  static File propertiesFile = new File(System.getProperty("user.home") + "/temp.fimk.properties");

  static void setup() {
    if (propertiesFile.exists()) {
      propertiesFile.delete();
    }

    Properties properties = loadProperties(new String[] {
        System.getProperty("user.home") + "/git/fimk/conf/fimk-default.properties",
        System.getProperty("user.home") + "/git/fimk/conf/fimk.properties"
    });

    writeConfigFile(properties, propertiesFile);

    System.setProperty("conf/fimk-default.properties", propertiesFile.getAbsolutePath());

    try {
      Properties loggingProperties = loadProperties(new String[] { System.getProperty("user.home") + "/git/fimk/conf/logging.properties"});
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      loggingProperties.store(outStream, "logging properties");
      ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
      java.util.logging.LogManager.getLogManager().readConfiguration(inStream);
      inStream.close();
      outStream.close();
    }
    catch (IOException e) {
      //throw new RuntimeException("Error loading logging properties", e);
      System.out.println("Error loading logging properties");
    }
  }

  static Properties loadProperties(String[] paths) {
    Properties properties = null;
    for (String path : paths) {
      properties = properties == null ? new Properties() : new Properties(
          properties);
      try {
        try (FileReader reader = new FileReader(new File(path))) {
          properties.load(reader);
          debugDump(path, properties);
        }
      }
      catch (IOException e) {
        //throw new RuntimeException("Error loading properties", e);
        System.out.println("Error loading properties");
      }
    }
    return properties;
  }

  static void writeConfigFile(Properties properties, File file) {
    OutputStream output = null;
    try {
      Properties temp = new Properties();
      Set<String> names = properties.stringPropertyNames();
      for (String key : names) {
        temp.setProperty(key, properties.getProperty(key));
      }
      output = new FileOutputStream(file);
      temp.store(output, null);
    }
    catch (IOException io) {
      io.printStackTrace();
    }
    finally {
      if (output != null) {
        try {
          output.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  static void debugDump(String title, Properties properties) {
    StringBuilder sb = new StringBuilder();
    sb.append("\n========================================================");
    sb.append("\nFIMK Configuration Settings - " + title);
    sb.append("\n========================================================");

    Set<String> names = properties.stringPropertyNames();
    for (String key : names) {
      sb.append("\n" + key + "=");
      String value = properties.getProperty(key);
      if (value.length() > 100) {
        value = value.substring(0, 100) + " ...";
      }
      sb.append(value);
    }
    System.out.println(sb.toString());
  }

}
