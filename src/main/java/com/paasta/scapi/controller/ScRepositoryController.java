package com.paasta.scapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.paasta.scapi.common.Common;
import com.paasta.scapi.common.Constants;
import com.paasta.scapi.common.exception.RestException;
import com.paasta.scapi.entity.ScRepository;
import com.paasta.scapi.model.Repository;
import com.paasta.scapi.service.ScRepositoryApiService;
import com.paasta.scapi.service.ScRepositoryDBService;
import com.paasta.scapi.service.ScUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sonia.scm.NotSupportedFeatuerException;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.RepositoryException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * paastaSourceControlApi
 * paastaSourceControlApi.repositories
 *
 * @author lena
 * @version 1.0
 * @since 6 /14/2017
 */
@RestController
@RequestMapping("/repositories")
public class ScRepositoryController {

    @Autowired
    private ScRepositoryApiService scRepositoryApiService;

    @Autowired
    private ScRepositoryDBService scRepositoryDBService;
    @Autowired
    private ScUserService scUserService;
    /**
     * Repository Creation.
     *
     * @apiVersion 0.1.0
     * @api {POST} /repositories     Repository Creation
     * @apiDescription Repository 생성
     * @apiName createRepository
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories' -i -X POST \
     * -H 'Content-Type: application/json' \
     * -d '{"name" : "repository name",
     * "description" : "description",
     * "type" : "git/svn",
     * "permissions" : [ {"groupPermission" : false,"name" : "user name","type" : "OWNER"}],
     * "public" : true/false,
     * "properties" : [ {"key" : "instance_id", "value" : "instanceId"}, {"key" : "create_user","value" : "userId"}]}'
     * @apiHeader {String} Content-Type Body Data Type
     * @apiParam {String} name 레파지토리 명
     * @apiParam {String} description 레파지토리 설명
     * @apiParam {String} type 레파지토리 계정 타입 (git/svn)
     * @apiParam {List} permissions 레파지토리 permission 리스트
     * @apiParam {boolean} public 공개/비공개 (true/false)
     * @apiParam {List} properties instance Id , 레파지토리 생성자 정보 설정
     */
    @PostMapping
    public ResponseEntity<Repository> createRepository(@RequestBody Repository request) throws RestException, JsonProcessingException {

        // Scm-Server api - Repository 생성 호출
        Repository repository = scRepositoryApiService.createRepositoryApi(request);

        // DB insert
        try {
            ScRepository scRepository = scRepositoryDBService.createRepositoryDB(repository);
        } catch (Exception e) {

            // Scm-Server api - Repository 삭제 호출
            scRepositoryApiService.deleteRepositoryApi(repository.getId());

            throw new RestException(Constants.RESULT_STATUS_FAIL);
        }

        return new ResponseEntity<Repository>(repository, HttpStatus.OK);
    }

    /**
     * Repository Inquiry - service instance
     *
     * @apiVersion 0.1.0
     * @api {Get} /repositories/admin     Repository Inquiry - super Admin
     * @apiDescription Service Instance별 Repository 조회
     * @apiName getAllRepositories
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/admin?instanceid={instanceid}&start={start}&end={end}' -i -X GET \
     * -H 'Content-Type: application/json'
     * @apiParam {String} instanceid service instance 아이디
     * @apiParam {number} start The start value for paging
     * @apiParam {number} end The end value for paging
     */
    @GetMapping("/admin")
    public ResponseEntity<Map<String,Object>> getAllRepositories(
            @RequestParam(value="instanceid", required=false) String instanceid,
            HttpServletRequest request
            ) throws RestException, Exception {

        Map map = request.getParameterMap();
        Map rtnMap = new HashMap();
        if(!Common.empty(map)){
            List<String> lstKey = new ArrayList(map .keySet());
            List<Object[]> lstValue = new ArrayList(map .values());
            for(int i = 0 ; i < lstKey.size(); i++){
                rtnMap.put(lstKey.get(i), lstValue.get(i)[0]);
            }

        }
        Map<String,Object> repositories = scRepositoryApiService. getAdminRepositories(
                instanceid, (String)rtnMap.getOrDefault("userid", ""),
                Integer.parseInt((String)rtnMap.getOrDefault("start", "-1")),
                Integer.parseInt((String)rtnMap.getOrDefault("end", "-1")),
                (String)rtnMap.getOrDefault("repoName", ""),  (String)rtnMap.getOrDefault("type", ""),
                (String)rtnMap.getOrDefault("reposort", ""));


//        repositories.stream().distinct().limit(10).collect(toList());

        return new ResponseEntity<Map<String, Object>>(repositories, HttpStatus.OK);
    }

    /**
     * Repository Inquiry - service instance & user
     *
     * @apiVersion 0.1.0
     * @api {Get} /repositories/user/{instanceid}    Repository Inquiry - user
     * @apiDescription Service Instance & User 별 Repository 조회
     * @apiName getUserRepositories
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/user/{instanceid}?username={username}&start={start}&end={end}' -i -X GET \
     * -H 'Content-Type: application/json'
     * @apiParam {String} instanceid service instance 아이디
     * @apiParam {String} username 사용자 명
     * @apiParam {number} start The start value for paging
     * @apiParam {number} end The end value for paging
     */
    @GetMapping(value = "/user/{instanceid}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getUserRepositories(
            @PathVariable String instanceid,
            @RequestParam(value = "username", required = false) String userid,
            @RequestParam(value = "type1", required = false) String type1,
            @RequestParam(value = "type2", required = false ) String type2,
            @RequestParam(value = "reposort", required = false, defaultValue = "lastModified_true") String reposort,
            @RequestParam(value = "repoName" , required = false) String repoName,
            @RequestParam(value = "start", required = false, defaultValue = "-1") int start,
            @RequestParam(value = "end", required = false, defaultValue = "-1") int end
    ) throws Exception {

//        Map<String, Object> repositories = scRepositoryApiService.getUserRepositories(instanceid, userid, start, end);
        Map<String, Object> repositories = scRepositoryApiService.getUserRepositories(instanceid, userid, start, end, repoName, type1, type2, reposort);
        repositories.put("username",userid);
        repositories.put("type1",type1);
        repositories.put("type2",type2);
        repositories.put("reposort",reposort);
        repositories.put("repoName",repoName);
        repositories.put("start",start);
        repositories.put("end",end);
        return new ResponseEntity<Map<String, Object>>(repositories, HttpStatus.OK);
    }

    /**
     * Repository Update.
     *
     * @apiVersion 0.1.0
     * @api {POST} /repositories     Repository Update
     * @apiDescription Repository 수정
     * @apiName updateRepository
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories' -i -X PUT \
     * -H 'Content-Type: application/json' \
     * -d '{"name" : "repository name",
     * "description" : "description",
     * "type" : "git/svn",
     * "permissions" : [ {"groupPermission" : false,"name" : "user name","type" : "OWNER"}],
     * "public" : true/false,
     * "properties" : [ {"key" : "instance_id", "value" : "instanceId"}, {"key" : "create_user","value" : "userId"}]}'
     * @apiHeader {String} Content-Type Body Data Type
     * @apiParam {String} name 레파지토리 명
     * @apiParam {String} description 레파지토리 설명
     * @apiParam {String} type 레파지토리 계정 타입 (git/svn)
     * @apiParam {List} permissions 레파지토리 permission 리스트
     * @apiParam {boolean} public 공개/비공개 (true/false)
     * @apiParam {List} properties instance Id , 레파지토리 생성자 정보 설정

    @PutMapping
    public ResponseEntity<String> updateRepository(@RequestBody Repository request) throws RestException, JsonProcessingException {


        // Scm-Server api - Repository 수정 호출
        this.scRepositoryApiService.updateRepositoryApi(request);
        // DB Update
        this.scRepositoryDBService.updateRepositoryDB(request);

        return new ResponseEntity<String>("{}", HttpStatus.OK);
    }
     */
    /**
     * Repository Delete
     *
     * @apiVersion 0.1.0
     * @api {Delete} /repositories/{id}    Repository Delete
     * @apiDescription Repository 삭제
     * @apiName deleteRepository
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/{id}' -i -X DELETE \
     * -H 'Content-Type: application/json'
     * @apiParam {String} id repository 아이디
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRepository(@PathVariable String id) throws RestException {
        try {
            // Scm-Server api - Repository 삭제 호출
            scRepositoryApiService.deleteRepositoryApi(id);
            // DB delete
            scRepositoryDBService.deleteRepositoryDB(id);
        }catch (Exception e){
            e.printStackTrace();
        }

        return new ResponseEntity<String>("삭제를 성공하였습니다.", HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<sonia.scm.repository.Repository> saveRepository(@PathVariable String id, @RequestBody Repository request) throws RestException, JsonProcessingException {

        // Scm-Server api - Repository 수정 호출
        sonia.scm.repository.Repository rtnRepository = scRepositoryApiService.updateRepository(id,request);

        return new ResponseEntity<sonia.scm.repository.Repository>( rtnRepository , HttpStatus.OK);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Repository> getRepositoryById(@PathVariable String id) throws RestException {

        // Scm-Server api - Repository 상세 조회 호출
        Repository repository = scRepositoryApiService.getRepositoryByIdApi(id);

        return new ResponseEntity<Repository>(repository, HttpStatus.OK);
    }

    @GetMapping("/name/{type}/{name}")
    public ResponseEntity<Map> getRepositoryByIdAndType(@PathVariable String name, @PathVariable String type) throws RestException {

        Map map = new HashMap();
        // Scm-Server api - Repository 상세 조회 호출
        sonia.scm.repository.Repository repository = scRepositoryApiService.scmRepositoryByNameType(type,name);
        map.put("repository", repository);
        return new ResponseEntity(map, HttpStatus.OK);
    }

    /**
     * Repository branches Inquery
     *
     * @apiVersion 0.1.8
     * @api {Get} /repositories/{id}/branches  Repository branches Inquery
     * @apiDescription Repository branches 조회
     * @apiName getBranches
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/{id}/branches' -i -X GET \
     * -H 'Content-Type: application/json'
     * @apiParam {String} id repository 아이디
     */
    @GetMapping(value = "/{id}/branches")
    public ResponseEntity<sonia.scm.repository.Branches> getBranches(@PathVariable String id) throws IOException, RepositoryException {
        sonia.scm.repository.Branches branches = scRepositoryApiService.getBranches(id);
        return new ResponseEntity<sonia.scm.repository.Branches>(branches, HttpStatus.OK);
}

    /**
     * Repository Tags Inquery
     *
     * @apiVersion 0.1.0
     * @api {Get} /repositories/{id}/tags  Repository Tags Inquery
     * @apiDescription Repository Tags 조회
     * @apiName getTags
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/{id}/tags' -i -X GET \
     * -H 'Content-Type: application/json'
     * @apiParam {String} id repository 아이디
     */
    @GetMapping(value = "/{id}/tags")
    public ResponseEntity<sonia.scm.repository.Tags> getTags(@PathVariable String id) throws RestException {
        sonia.scm.repository.Tags tags = scRepositoryApiService.getTags(id);
        return new ResponseEntity<sonia.scm.repository.Tags>(tags, HttpStatus.OK);
    }

    /**
     * Repository browse Inquery
     *
     * @apiVersion 0.1.0
     * @api {Get} /repositories/{id}/browse  Repository browse Inquery
     * @apiDescription Repository browse 조회
     * @apiName getBrowse
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/{id}/browse' -i -X GET \
     * -H 'Content-Type: application/json'
     * @apiParam {String} id repository 아이디
     *     disableLastCommit	query	true disables fetch of last commit message	false	boolean
     *     disableSubRepositoryDetection	query	true disables sub repository detection	false	boolean
     *     path	query	the path of the folder
     *     recursive	query	true to enable recursive browsing	false	boolean
     *     revision	query	the revision of the file
     */
    @GetMapping("/{id}/browse")
    public ResponseEntity<BrowserResult> getBrowse(@PathVariable String id,
                                                   @RequestParam(value = "disableLastCommit", required = false) boolean disableLastCommit,
                                                   @RequestParam(value = "disableSubRepositoryDetection", required = false) boolean disableSubRepositoryDetection,
                                                   @RequestParam(value = "path", required = false, defaultValue = "") String path,
                                                   @RequestParam(value = "recursive", required = false) boolean recursive,
                                                   @RequestParam(value = "revision", required = false, defaultValue = "") String revision
                                                    ) throws IOException, NotSupportedFeatuerException {
        BrowserResult browserResult = scRepositoryApiService.getBrowseByParam(id,disableLastCommit,disableSubRepositoryDetection,path,recursive,revision);
        return new ResponseEntity<BrowserResult>(browserResult, HttpStatus.OK);
    }

    /**
     * Repository changesets Inquery
     *
     * @apiVersion 0.1.0
     * @api {Get} /repositories/{id}/changesets  Repository changesets Inquery
     * @apiDescription Repository - 변경된 리스트 목록 조회
     * @apiName getChangesets
     * @apiGroup Repositories
     * @apiExample {curl} Example usage:
     * curl 'http://localhost:9091/repositories/{id}/getChangesets' -i -X GET \
     * -H 'Content-Type: application/json'
     * @apiParam {String} id repository 아이디
     */
    @GetMapping("/{id}/changesets")
    public ResponseEntity<ChangesetPagingResult> getChangesets(@PathVariable String id) throws RestException, NotSupportedFeatuerException {
        ChangesetPagingResult changesets = scRepositoryApiService.getChangesets(id);
        return new ResponseEntity<ChangesetPagingResult>(changesets, HttpStatus.OK);
    }

    /**
     * Repository changesets Inquery
     *
     * curl 'http://localhost:9091/repositories/{id}/contents?path=.gitignore&_dc=1506392871640' -i -X GET \
     * @apiParam {String} id repository 아이디
     */
    @GetMapping("/{id}/content")
    @ResponseBody
//    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public ResponseEntity<byte[]> getContents(@PathVariable String id
                                     , @RequestParam(value = "revision", required = false) String revision
                                     , @RequestParam(value = "_dc", required = false) String _dc
                                     , @RequestParam(value = "path") String path) throws NotSupportedFeatuerException, IOException{
        // REX-TEST
        return scRepositoryApiService.getContent(id, revision, path, _dc);
    }

}