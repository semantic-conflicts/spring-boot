package org.springframework.boot.loader.archive;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import org.springframework.boot.loader.util.AsciiBytes;
/** 
 * {@link Archive} implementation backed by an exploded archive directory.
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ExplodedArchive extends Archive {
  public static Set<String> SKIPPED_NAMES=new HashSet<String>(Arrays.asList(".",".."));
  public static AsciiBytes MANIFEST_ENTRY_NAME=new AsciiBytes("META-INF/MANIFEST.MF");
  public File root;
  public Map<AsciiBytes,Entry> entries=new LinkedHashMap<AsciiBytes,Entry>();
  public Manifest manifest;
  public boolean filtered=false;
  /** 
 * Create a new  {@link ExplodedArchive} instance.
 * @param root the root folder
 */
  public ExplodedArchive(  File root){
    this(root,true);
  }
  /** 
 * Create a new  {@link ExplodedArchive} instance.
 * @param root the root folder
 * @param recursive if recursive searching should be used to locate the manifest.Defaults to  {@code true}, folders with a large tree might want to set this to {code false}.
 */
  public ExplodedArchive(  File root,  boolean recursive){
    if (!root.exists() || !root.isDirectory()) {
      throw new IllegalArgumentException("Invalid source folder " + root);
    }
    this.root=root;
    buildEntries(root,recursive);
    this.entries=Collections.unmodifiableMap(this.entries);
  }
  public ExplodedArchive(  File root,  Map<AsciiBytes,Entry> entries){
    this.root=root;
    this.filtered=true;
    this.entries=Collections.unmodifiableMap(entries);
  }
  public void buildEntries(  File file,  boolean recursive){
    if (!file.equals(this.root)) {
      String name=file.toURI().getPath().substring(this.root.toURI().getPath().length());
      FileEntry entry=new FileEntry(new AsciiBytes(name),file);
      this.entries.put(entry.getName(),entry);
    }
    if (file.isDirectory()) {
      File[] files=file.listFiles();
      if (files == null) {
        return;
      }
      for (      File child : files) {
        if (!SKIPPED_NAMES.contains(child.getName())) {
          if (file.equals(this.root) || recursive || file.getName().equals("META-INF")) {
            buildEntries(child,recursive);
          }
        }
      }
    }
  }
  @Override public URL getUrl() throws MalformedURLException {
    FilteredURLStreamHandler handler=this.filtered ? new FilteredURLStreamHandler() : null;
    return new URL("file","",-1,this.root.toURI().toURL().getPath(),handler);
  }
  @Override public Manifest getManifest() throws IOException {
    if (this.manifest == null && this.entries.containsKey(MANIFEST_ENTRY_NAME)) {
      FileEntry entry=(FileEntry)this.entries.get(MANIFEST_ENTRY_NAME);
      FileInputStream inputStream=new FileInputStream(entry.getFile());
      try {
        this.manifest=new Manifest(inputStream);
      }
  finally {
        inputStream.close();
      }
    }
    return this.manifest;
  }
  @Override public List<Archive> getNestedArchives(  EntryFilter filter) throws IOException {
    List<Archive> nestedArchives=new ArrayList<Archive>();
    for (    Entry entry : getEntries()) {
      if (filter.matches(entry)) {
        nestedArchives.add(getNestedArchive(entry));
      }
    }
    return Collections.unmodifiableList(nestedArchives);
  }
  @Override public Collection<Entry> getEntries(){
    return Collections.unmodifiableCollection(this.entries.values());
  }
  public Archive getNestedArchive(  Entry entry) throws IOException {
    File file=((FileEntry)entry).getFile();
    return (file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
  }
  @Override public Archive getFilteredArchive(  EntryRenameFilter filter) throws IOException {
    Map<AsciiBytes,Entry> filteredEntries=new LinkedHashMap<AsciiBytes,Archive.Entry>();
    for (    Map.Entry<AsciiBytes,Entry> entry : this.entries.entrySet()) {
      AsciiBytes filteredName=filter.apply(entry.getKey(),entry.getValue());
      if (filteredName != null) {
        filteredEntries.put(filteredName,new FileEntry(filteredName,((FileEntry)entry.getValue()).getFile()));
      }
    }
    return new ExplodedArchive(this.root,filteredEntries);
  }
public class FileEntry implements Entry {
    public AsciiBytes name;
    public File file;
    FileEntry(    AsciiBytes name,    File file){
      this.name=name;
      this.file=file;
    }
    public File getFile(){
      return this.file;
    }
    @Override public boolean isDirectory(){
      return this.file.isDirectory();
    }
    @Override public AsciiBytes getName(){
      return this.name;
    }
    public FileEntry(){
    }
  }
  /** 
 * {@link URLStreamHandler} that respects filtered entries.
 */
public class FilteredURLStreamHandler extends URLStreamHandler {
    @Override public URLConnection openConnection(    URL url) throws IOException {
      String name=url.getPath().substring(ExplodedArchive.this.root.toURI().getPath().length());
      if (ExplodedArchive.this.entries.containsKey(new AsciiBytes(name))) {
        return new URL(url.toString()).openConnection();
      }
      return new FileNotFoundURLConnection(url,name);
    }
    public FilteredURLStreamHandler(){
    }
  }
  /** 
 * {@link URLConnection} used to represent a filtered file.
 */
public static class FileNotFoundURLConnection extends URLConnection {
    public String name;
    FileNotFoundURLConnection(    URL url,    String name){
      super(url);
      this.name=name;
    }
    @Override public void connect() throws IOException {
      throw new FileNotFoundException(this.name);
    }
    public FileNotFoundURLConnection(){
    }
  }
  public ExplodedArchive(){
  }
}
