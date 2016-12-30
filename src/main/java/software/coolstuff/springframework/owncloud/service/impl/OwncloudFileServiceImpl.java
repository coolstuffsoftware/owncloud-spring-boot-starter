package software.coolstuff.springframework.owncloud.service.impl;

import java.net.URL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.webdav.WebdavFileSystemConfigBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;

import software.coolstuff.springframework.owncloud.service.api.OwncloudFileService;

@Scope(WebApplicationContext.SCOPE_SESSION)
class OwncloudFileServiceImpl implements OwncloudFileService {

  public final static String DEFAULT_ROOT = ".";

  private final static String FS_SCHEME_WEBDAV = "webdav";
  private final static String FS_SCHEME_RAM = "ram";
  private final static String FS_SCHEME_FILE = "file";

  private final static String CONFIG_RESOURCE = "/META-INF/owncloud-vfs-providers.xml";

  private final static String WEBDAV_URI_SCHEME = "webdav://{server}:{port}/remote.php/dav/files/{username}/";
  private final static String FILE_URI_SCHEME = "file:{root}/{username}";

  @Autowired
  private OwncloudProperties owncloudProperties;

  private StandardFileSystemManager fileSystemManager;

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    fileSystemManager = new StandardFileSystemManager();
    URL configURL = fileSystemManager.getClass().getResource(CONFIG_RESOURCE);
    fileSystemManager.setConfiguration(configURL);
    fileSystemManager.init();

    String scheme = FS_SCHEME_WEBDAV;
    if (OwncloudUtils.isResourceLocation(owncloudProperties.getLocation())) {
      scheme = FS_SCHEME_RAM;
      if (OwncloudUtils.isFileResourceLocation(owncloudProperties.getLocation())) {
        scheme = FS_SCHEME_FILE;
      }
    }

    final FileSystemOptions options = new FileSystemOptions();
    DefaultFileSystemConfigBuilder fileConfigBuilder = DefaultFileSystemConfigBuilder.getInstance();
    String rootURI = DEFAULT_ROOT;
    switch (scheme) {
      case FS_SCHEME_WEBDAV:
        HttpFileSystemConfigBuilder webdavConfigBuilder = WebdavFileSystemConfigBuilder.getInstance();
        break;
      case FS_SCHEME_FILE:
        rootURI = new String(FILE_URI_SCHEME);
        String root = StringUtils.defaultIfEmpty(owncloudProperties.getFileResourceRoot(), DEFAULT_ROOT);
        rootURI = StringUtils.replace(rootURI, "{root}", root);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        rootURI = StringUtils.replace(rootURI, "{username}", username);
        break;
    }
    fileConfigBuilder.setRootURI(options, rootURI);
  }

  @PreDestroy
  public void destroy() throws Exception {
    fileSystemManager.close();
  }

  @Override
  public FileObject resolveFile(String filename) {
    // TODO Auto-generated method stub
    return null;
  }

}
