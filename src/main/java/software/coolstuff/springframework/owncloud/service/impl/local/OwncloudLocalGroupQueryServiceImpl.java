package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupQueryService;

@RequiredArgsConstructor
@Slf4j
@Deprecated
public class OwncloudLocalGroupQueryServiceImpl implements OwncloudGroupQueryService {

  private final OwncloudLocalUserDataService localDataService;

}
