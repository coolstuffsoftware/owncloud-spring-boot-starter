package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@RequiredArgsConstructor
@Slf4j
@Deprecated
public class OwncloudLocalUserQueryServiceImpl implements OwncloudUserQueryService {

  private final OwncloudLocalUserDataService localDataService;

}
