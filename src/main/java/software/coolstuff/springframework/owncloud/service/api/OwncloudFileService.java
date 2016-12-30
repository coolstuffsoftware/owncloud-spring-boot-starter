package software.coolstuff.springframework.owncloud.service.api;

import org.apache.commons.vfs2.FileObject;

public interface OwncloudFileService {

  FileObject resolveFile(String filename);

}
