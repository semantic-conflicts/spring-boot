package org.springframework.boot.loader.archive;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.archive.Archive.Entry;
import static org.assertj.core.api.Assertions.assertThat;
/** 
 * Tests for  {@link ExplodedArchive}.
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ExplodedArchiveTests {
  @Rule public TemporaryFolder temporaryFolder=new TemporaryFolder();
  public File rootFolder;
  public ExplodedArchive archive;
  @Before public void setup() throws Exception {
    File file=this.temporaryFolder.newFile();
    TestJarCreator.createTestJar(file);
    this.rootFolder=this.temporaryFolder.newFolder();
    JarFile jarFile=new JarFile(file);
    Enumeration<JarEntry> entries=jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry=entries.nextElement();
      File destination=new File(this.rootFolder.getAbsolutePath() + File.separator + entry.getName());
      destination.getParentFile().mkdirs();
      if (entry.isDirectory()) {
        destination.mkdir();
      }
 else {
        copy(jarFile.getInputStream(entry),new FileOutputStream(destination));
      }
    }
    this.archive=new ExplodedArchive(this.rootFolder);
    jarFile.close();
  }
  public void copy(  InputStream in,  OutputStream out) throws IOException {
    byte[] buffer=new byte[1024];
    int len=in.read(buffer);
    while (len != -1) {
      out.write(buffer,0,len);
      len=in.read(buffer);
    }
  }
  @Test public void getManifest() throws Exception {
    assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
  }
  @Test public void getEntries() throws Exception {
    Map<String,Archive.Entry> entries=getEntriesMap(this.archive);
    assertThat(entries.size()).isEqualTo(10);
  }
  @Test public void getUrl() throws Exception {
    URL url=this.archive.getUrl();
    assertThat(new File(URLDecoder.decode(url.getFile(),"UTF-8"))).isEqualTo(this.rootFolder);
  }
  @Test public void getNestedArchive() throws Exception {
    Entry entry=getEntriesMap(this.archive).get("nested.jar");
    Archive nested=this.archive.getNestedArchive(entry);
    assertThat(nested.getUrl().toString()).isEqualTo("jar:" + this.rootFolder.toURI() + "nested.jar!/");
  }
  @Test public void nestedDirArchive() throws Exception {
    Entry entry=getEntriesMap(this.archive).get("d/");
    Archive nested=this.archive.getNestedArchive(entry);
    Map<String,Entry> nestedEntries=getEntriesMap(nested);
    assertThat(nestedEntries.size()).isEqualTo(1);
    assertThat(nested.getUrl().toString()).isEqualTo("file:" + this.rootFolder.toURI().getPath() + "d/");
  }
  @Test public void getNonRecursiveEntriesForRoot() throws Exception {
    ExplodedArchive archive=new ExplodedArchive(new File("/"),false);
    Map<String,Archive.Entry> entries=getEntriesMap(archive);
    assertThat(entries.size()).isGreaterThan(1);
  }
  @Test public void getNonRecursiveManifest() throws Exception {
    ExplodedArchive archive=new ExplodedArchive(new File("src/test/resources/root"));
    assertThat(archive.getManifest()).isNotNull();
    Map<String,Archive.Entry> entries=getEntriesMap(archive);
    assertThat(entries.size()).isEqualTo(4);
  }
  @Test public void getNonRecursiveManifestEvenIfNonRecursive() throws Exception {
    ExplodedArchive archive=new ExplodedArchive(new File("src/test/resources/root"),false);
    assertThat(archive.getManifest()).isNotNull();
    Map<String,Archive.Entry> entries=getEntriesMap(archive);
    assertThat(entries.size()).isEqualTo(3);
  }
  @Test public void getResourceAsStream() throws Exception {
    ExplodedArchive archive=new ExplodedArchive(new File("src/test/resources/root"));
    assertThat(archive.getManifest()).isNotNull();
    URLClassLoader loader=new URLClassLoader(new URL[]{archive.getUrl()});
    assertThat(loader.getResourceAsStream("META-INF/spring/application.xml")).isNotNull();
    loader.close();
  }
  @Test public void getResourceAsStreamNonRecursive() throws Exception {
    ExplodedArchive archive=new ExplodedArchive(new File("src/test/resources/root"),false);
    assertThat(archive.getManifest()).isNotNull();
    URLClassLoader loader=new URLClassLoader(new URL[]{archive.getUrl()});
    assertThat(loader.getResourceAsStream("META-INF/spring/application.xml")).isNotNull();
    loader.close();
  }
  public Map<String,Archive.Entry> getEntriesMap(  Archive archive){
    Map<String,Archive.Entry> entries=new HashMap<String,Archive.Entry>();
    for (    Archive.Entry entry : archive) {
      entries.put(entry.getName().toString(),entry);
    }
    return entries;
  }
  public ExplodedArchiveTests(){
  }
}
