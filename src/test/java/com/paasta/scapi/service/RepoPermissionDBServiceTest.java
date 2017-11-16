package com.paasta.scapi.service;

import com.paasta.scapi.common.Constants;
import com.paasta.scapi.common.util.DateUtil;
import com.paasta.scapi.entity.RepoPermission;
import com.paasta.scapi.entity.ScRepository;
import com.paasta.scapi.entity.ScUser;
import com.paasta.scapi.model.User;
import com.paasta.scapi.repository.RepoPermissionRepository;
import com.paasta.scapi.repository.ScRepositoryRepository;
import com.paasta.scapi.repository.ScUserRepository;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import sonia.scm.client.ScmClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by SEJI on 2017-10-16.
 */
@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RepoPermissionDBServiceTest extends CommonServiceTest{

    @Before
    public void setUp() {
        setUpMockUtil();
    }

    @Test
    public void searchPermisionByUserIdAndRepositoryId() throws Exception {

        rtnList = new ArrayList();
        Map map = new HashMap();
        map.put("userId",searchUserId);
        map.put("userName",userName);
        map.put("userEmail",userMail);
        map.put("userPermission",sRepoPermission);
        map.put("userPermissionNo",iRepoNo);
        rtnList.add(map);

       List<Map> mathRtnList = repoPermissionDBService.searchPermisionByUserIdAndRepositoryId(searchUserId,repoScmId);

       Assert.assertEquals(mathRtnList,rtnList);

    }
    @Test
    public void searchPermisionByUserIdAndInstanceId() throws Exception {
        rtnList = new ArrayList<>();
        Map map = new HashMap();
        map.put("no", getInstanceNo);
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userEmail", userMail);
        map.put("userRepoRole", userRepoRole );
        map.put("userCreateYn", userCreateYn);
        map.put("userPermission", sRepoPermission);
        map.put("userPermissionNo", getPermissionNo);
        map.put("userModifiedDate",  DateUtil.rtnFormatString(Constants.DATE_FORMAT_1, userModifiedDate));
        map.put("userCreatedDate",  DateUtil.rtnFormatString(Constants.DATE_FORMAT_1, userCreatedDate));
        rtnList.add(map);
        List<Map> mathRtnList = repoPermissionDBService.searchPermisionByUserIdAndInstanceId(searchUserId,instanceId,repoScmId);
        Assert.assertEquals(mathRtnList,rtnList);
    }

}
