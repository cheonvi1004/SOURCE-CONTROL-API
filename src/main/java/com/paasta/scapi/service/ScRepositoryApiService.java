package com.paasta.scapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paasta.scapi.common.Common;
import com.paasta.scapi.common.Constants;
import com.paasta.scapi.common.exception.RestException;
import com.paasta.scapi.common.util.PropertiesUtil;
import com.paasta.scapi.common.util.RestClientUtil;
import com.paasta.scapi.model.Permission;
import com.paasta.scapi.model.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sonia.scm.NotSupportedFeatuerException;
import sonia.scm.client.ClientChangesetHandler;
import sonia.scm.client.RepositoryClientHandler;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Tags;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

/**
 * Created by lena on 2017-06-14.
 */
@PropertySource("classpath:scapi.properties")
@Service
public class ScRepositoryApiService extends  CommonService{

    @Autowired
    RestClientUtil restClientUtil;

    @Autowired
    ScRepositoryDBService scRepositoryDBService;

    @Autowired
    PropertiesUtil propertiesUtil;

    private Repository scmRepositoryByNameTypePrivate(String type, String name) {

        HttpEntity<Object> entity = this.restClientUtil.restCommonHeaders(null);
        String url = this.propertiesUtil.getApi_repo_type_name().replace("{type}", type);
        url = url.replace("{name}", name);

        ResponseEntity<Repository> response = this.restClientUtil.callRestApi(HttpMethod.GET, url, entity, Repository.class);

        return response.getBody();

    }


    
    public Map<String, Object> getAllRepositories(String instanceid, int start, int end) throws Exception {

        Map<String, Object> resultMap = new HashMap();

        List<Repository> InstanceRepositories = this.getRepositoryByInstanceId(instanceid, "");

        int totCnt = InstanceRepositories.size();

        if (start == 0 || start > end || start > totCnt) {
            throw new RestException("page error");
        } else if (start >= 1 && end > 0 && start <= end && start <= totCnt) {
            resultMap.put("repositories", InstanceRepositories.stream().skip(start - 1).limit(end).collect(toList()));
        } else {
            resultMap.put("repositories", InstanceRepositories);
        }

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("totalCnt", totCnt);
        pageInfo.put("startCnt", start);
        pageInfo.put("endCnt", end >= totCnt ? totCnt : end);
        resultMap.put("pageInfo", pageInfo);
        return resultMap;
    }


    
    public Map<String, Object> getUserRepositories(String instanceid, String userid, int start, int end, String repoName, String type1, String type2, String reposort) throws Exception {
        Map<String, Object> resultMap = new HashMap();
        logger.debug("getUserRepositories::" + instanceid + "::userid::" + userid + "::start::" + start + "::end::" + end + "::repoName::" + repoName + "::type1::" + type1 + "::type2::" + type2 + "::reposort::" + reposort);
        try {
            // 서비스 인스턴스별 repository
            List<Repository> InstanceRepositories = getRepositoryByInstanceId(instanceid, reposort);
            List<Repository> repositories = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();

            if (!StringUtils.isEmpty(instanceid)) {
                // Repository <> User 권한 Check : 참여되어있는 Repository 조회
                InstanceRepositories.forEach((Repository e) -> {
                    Repository repository = objectMapper.convertValue(e, Repository.class);
                    boolean[] bRtn = {true};
                    boolean[] bType2 = {true};
                    //사용자 아이디 검색 하지 않음
                    /**
                     if(Common.notEmpty(userid)){
                     if (!repository.getPermissions().isEmpty() && repository.getPermissions().stream().filter(pe -> userid.equals(pe.getName())).count() > 0) {
                     bRtn = false;
                     }
                     }
                     */
                    //레파지토리 이름 검색
                    if (Common.notEmpty(repoName)) {
                        if (!repository.getName().contains(repoName)) {
                            bRtn[0] = false;
                        }
                    }
                    //레파지 토리 타입 검색(git, svn)
                    if (Common.notEmpty(type1)) {
                        if (!repository.getType().equals(type1)) {
                            bRtn[0] = false;
                        }
                    }
                    //레파지토리 권한 검색 OWNER, READ, WRITE
                    if (Common.notEmpty(type2)) {
                        String[] lstType2 = type2.split("_");
                        bType2[0] = false;
                        List<Permission> permissions = repository.getPermissions();
                        for (int i = 0; i < permissions.size(); i++) {
                            Permission permission = permissions.get(i);
                            for (int j = 0; j < lstType2.length; j++) {
                                String sType2 = lstType2[j];
                                if (permission.getName().equals(userid) && permission.getType().equals(sType2.toString()))
                                    bType2[0] = true;
                            }
                        }
                    }
                    if (bRtn[0] && bType2[0]) {
                        repositories.add(repository);
                    }
                });
            } else {
                repositories.forEach(e -> {
                    Repository repository = objectMapper.convertValue(e, Repository.class);
                    repositories.add(repository);
                });
            }
            int totCnt = repositories.size();
            Map<String, Object> pageInfo = new HashMap<>();
            if (start == 0 || start > end) {
                //            Throwable cause = e.getCause();
                throw new RuntimeException("page error");
            } else if (start >= 1 && end > 0 && start <= end && start <= totCnt) {
                resultMap.put("repositories", repositories.stream().skip(start - 1).limit(end).collect(toList()));
            } else {
                resultMap.put("repositories", repositories);
            }
            pageInfo.put("totalCnt", totCnt);
            pageInfo.put("startCnt", start);
            pageInfo.put("endCnt", end >= totCnt ? totCnt : end);
            resultMap.put("pageInfo", pageInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }


    
    public Repository getRepositoryByIdApi(String id) throws RestException {
        // scm-manager search repository
        HttpEntity<Object> entity = this.restClientUtil.restCommonHeaders(null);
        String url = propertiesUtil.getApi_repo_id().replace("{id}", id);
        ResponseEntity<Repository> response = restClientUtil.callRestApi(HttpMethod.GET, url, entity, Repository.class);

        Repository repository = response.getBody();
        String repositoryUrl = repository.getUrl();

        int istartUrl = repositoryUrl.indexOf(":",7);
        repositoryUrl = repositoryUrl.substring(istartUrl);
        repositoryUrl = repositoryUrl.substring(repositoryUrl.indexOf("/")+4,repositoryUrl.length());
        repository.setUrl(propertiesUtil.getBase_url() + repositoryUrl);

        return repository;
    }


    
    public Repository createRepositoryApi(Repository request) throws RestException, JsonProcessingException {

        // step1. scm-manager create repository
        // 1-1. create repository
        scmCreateRepository(request);

        // step2. scm-managert get created repository info
        Repository repository = scmRepositoryByNameTypePrivate(request.getType(), request.getName());

        return repository;
    }


//    
//    public void updateRepositoryApi(Repository request) throws RestException, JsonProcessingException {
//        // step1. scm-manager update repository
//        this.scmUpdateRepository(request);
//
//    }


    
    public void deleteRepositoryApi(String id) {

        // scm-manager delete repository
        HttpEntity<Object> entity = this.restClientUtil.restCommonHeaders(null);
        String url = propertiesUtil.getApi_repo_id().replace("{id}", id);
        ResponseEntity<String> response = this.restClientUtil.callRestApi(HttpMethod.DELETE, url, entity, String.class);

    }


    
    public Map<String, Object> getAdminRepositories(String instanceid, String userid, int start, int end, String repoName, String type, String reposort) throws Exception {
        Map<String, Object> resultMap = new HashMap();

        // 서비스 인스턴스별 repository
        List<Repository> InstanceRepositories = getRepositoryByInstanceId(instanceid, reposort);
        List<Repository> repositories = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if (!StringUtils.isEmpty(instanceid)) {
            InstanceRepositories.forEach((Repository e) -> {
                Repository repository = objectMapper.convertValue(e, Repository.class);
                if (!repository.getPermissions().isEmpty()) {
                    boolean bRtn = true;
                    if (Common.notEmpty(repoName)) {
                        if (!repository.getName().contains(repoName)) {
                            bRtn = false;
                        }
                    }
                    if (bRtn) {
                        repositories.add(repository);
                    }
                }
            });
        } else {
            repositories.forEach(e -> {
                Repository repository = objectMapper.convertValue(e, Repository.class);
                repositories.add(repository);
            });
        }

        int totCnt = repositories.size();
        Map<String, Object> pageInfo = new HashMap<>();
        if (start == 0 || start > end) {
//            Throwable cause = e.getCause();
            throw new RuntimeException("page error");
        } else if (start >= 1 && end > 0 && start <= end && start <= totCnt) {
            resultMap.put("repositories", repositories.stream().skip(start - 1).limit(end).collect(toList()));
        } else {
            resultMap.put("repositories", repositories);
        }

        pageInfo.put("totalCnt", totCnt);
        pageInfo.put("startCnt", start);
        pageInfo.put("endCnt", end >= totCnt ? totCnt : end);
        resultMap.put("pageInfo", pageInfo);

        return resultMap;
    }


    // [ private Method ]=================================================================================================
    private List<Repository> scmAllRepositories(String sSort) throws Exception {
        // 모든 Repository 조회
        HttpEntity<Object> entity = restClientUtil.restCommonHeaders(null);

        ParameterizedTypeReference<List<Repository>> responseType = new ParameterizedTypeReference<List<Repository>>() {
        };
        ResponseEntity<List<Repository>> response = restClientUtil.callRestApiReturnObjList(HttpMethod.GET, this.propertiesUtil.getApi_repo() + sSort, entity, responseType);

        List<Repository> repositories = response.getBody();

        return repositories;
    }


    public sonia.scm.repository.Repository scmRepositoryByNameType(String type, String name) {

        RepositoryClientHandler repositoryClientHandler = scmClientSession().getRepositoryHandler();
        sonia.scm.repository.Repository repository = repositoryClientHandler.get(type, name);

        return repository;

    }


    public List<Repository> getRepositoryByInstanceId(String instanceid, String reposprt) throws Exception {
        String sSort = "";
        if (Common.notEmpty(reposprt)) {
            String[] sort = reposprt.split("_");
            if (sort.length != 0) {
                sSort = "?sortby=" + sort[0] + "&desc=" + sort[1];
//                sSort = "?desc=" + sort[1];
            }
        }
        List<Repository> repositories = scmAllRepositories(sSort);

        // 인스턴스 아이디 존재하지 않을 경우, 모든 레파지토리 반환
        List<Repository> repositoryList = new ArrayList<Repository>();

        // instance ID 가 존재하지 않는 경우, 모든 레파지토리 목록 반환
        if (StringUtils.isEmpty(instanceid)) {
            return repositories;
        }

        repositories.forEach(e -> {
            if (e.getProperties() != null && !e.getProperties().isEmpty() && e.getProperties().stream().filter(pr -> Constants.REPO_PROPERTIES_INSTANCE_ID.equals(pr.get(Constants.PROPERTIES_KEY)) && instanceid.equals(pr.get(Constants.PROPERTIES_VALUE))).count() > 0) {
                repositoryList.add(e);
            }
        });

        return repositoryList;
    }


    private ResponseEntity<String> scmCreateRepository(Repository request) {
        ResponseEntity<String> response;
        try {
            logger.debug(request.toString());
            request.setCreationDate(new Long(0));
            request.setLastModified(new Date().getTime());
            HttpEntity<Object> entity = restClientUtil.restCommonHeaders(request);
            return restClientUtil.callRestApi(HttpMethod.POST, this.propertiesUtil.getApi_repo(), entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<String>(HttpStatus.FORBIDDEN);
        }
    }


    private ResponseEntity<String> scmUpdateRepository(Repository request) {

        HttpEntity<Object> entity = this.restClientUtil.restCommonHeaders(request);
        String url = this.propertiesUtil.getApi_repo_id().replace("{id}", request.getId());
        ResponseEntity<String> response = this.restClientUtil.callRestApi(HttpMethod.PUT, url, entity, String.class);

        return response;
    }


    
    public sonia.scm.repository.Branches getBranches(String id) {
        HttpEntity<Object> entity = restClientUtil.restCommonHeader(null);
        String url = propertiesUtil.getApi_repo_branches().replace("{id}", id);
        ResponseEntity<sonia.scm.repository.Branches> response = restClientUtil.callRestApi(HttpMethod.GET, url, entity, sonia.scm.repository.Branches.class);
        sonia.scm.repository.Branches repositories = response.getBody();
        return repositories;
    }


    
    public Tags getTags(String id) {
        sonia.scm.repository.Repository repository = scmClientSession().getRepositoryHandler().get(id);
        Tags tags = scmClientSession().getRepositoryHandler().getTags(repository);
        return tags;
    }


//
//    
//    public BrowserResult getBrowse(String id) {
//        HttpEntity<Object> entity = restClientUtil.restCommonHeaders(null);
//        String url = propertiesUtil.getApi_repo_browse().replace("{id}", id);
//        ResponseEntity<BrowserResult> response = restClientUtil.callRestApi(HttpMethod.GET, url, entity, BrowserResult.class);
//        BrowserResult repositories = (BrowserResult) response.getBody();
//        return repositories;
//    }


    
    public BrowserResult getBrowseByParam(String id, boolean disableLastCommit, boolean disableSubRepositoryDetection, String path, boolean recursive, String revision) throws NotSupportedFeatuerException, IOException {
        HttpEntity<Object> entity = restClientUtil.restCommonHeaders(null);
        String url = propertiesUtil.getApi_repo_browse().replace("{id}", id);
        String param = "?disableLastCommit=" + disableLastCommit + "&disableSubRepositoryDetection=" + disableSubRepositoryDetection + "&path=" + path + "&recursive=" + recursive + "&revision=" + revision;
        url = url + param;
        logger.debug("url:::" + url);
        ResponseEntity<BrowserResult> response = restClientUtil.callRestApi(HttpMethod.GET, url, entity, BrowserResult.class);
        BrowserResult repositories = response.getBody();
        return repositories;
    }


    
    public ChangesetPagingResult getChangesets(String id) throws NotSupportedFeatuerException {
        RepositoryClientHandler repositoryHandler = scmClientSession().getRepositoryHandler();
        sonia.scm.repository.Repository repository = repositoryHandler.get(id);
        ClientChangesetHandler changesetHandler = repositoryHandler.getChangesetHandler(repository);
        // get 20 changesets started by 0
        ChangesetPagingResult changesets = changesetHandler.getChangesets(0, 10);
        for (sonia.scm.repository.Changeset c : changesets) {
            System.out.println(c.getId() + ": " + c.getDescription());
        }
        return changesets;
    }


    
    @Transactional
    public sonia.scm.repository.Repository updateRepository(String id, Repository repository) {
        try {
            RepositoryClientHandler repositoryClientHandler = scmClientSession().getRepositoryHandler();
            //조회
            sonia.scm.repository.Repository mofiyRepository = repositoryClientHandler.get(id);

            //변경사항 저장
            mofiyRepository.setDescription(repository.getDescription());
            mofiyRepository.setPublicReadable(repository.isPublic_());
            repositoryClientHandler.modify(mofiyRepository);
            scRepositoryDBService.updateRepositoryDB(repository);
            return mofiyRepository;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
    @Transactional
    public void createRepositoryApiFile(Repository repository){
        RepositoryClientHandler repositoryClientHandler =  scmClientSession().getRepositoryHandler();
        ImportBundleRequest importBundleRequest = new ImportBundleRequest("file", "README.MD")
        repositoryClientHandler.importFromBundle(importBundleRequest);
    }*/

    
    public ResponseEntity<byte[]> getContent(String id, String revision, String path, String _dc) throws NotSupportedFeatuerException, IOException {
        String url = propertiesUtil.getApi_repository_id_content_path_revision().replace("{id}", id).replace("{path}", path);
        url = url.replace("{revision}", (String) Common.notNullrtnByobj(revision, ""));
        url = url.replace("{_dc}", (String) Common.notNullrtnByobj(_dc, ""));

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.add("Authorization", this.propertiesUtil.getBasicAuth());
        headers.add(ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return this.restClientUtil.callRestApi(HttpMethod.GET, url, new HttpEntity<>(null, headers), byte[].class);
    }

}
