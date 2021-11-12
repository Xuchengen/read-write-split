package com.github.xuchengen.rws.web;

import com.github.pagehelper.PageInfo;
import com.github.xuchengen.rws.biz.UserService;
import com.github.xuchengen.rws.dao.model.UserDO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "用户模块")
@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Resource(name = "userService")
    private UserService userService;

    @ApiOperation(value = "分页查询")
    @PostMapping(value = "/queryByPage")
    public PageInfo<UserDO> queryByPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.queryByPage(pageNum, pageSize);
    }

    @ApiOperation(value = "按用户ID查询")
    @PostMapping(value = "/getById")
    public UserDO getById(@RequestParam(defaultValue = "1") Long id) {
        return userService.getById(id);
    }

    @ApiOperation(value = "创建用户")
    @PostMapping(value = "/createUser")
    public boolean createUser(@RequestParam String name,
                              @RequestParam String phone) {
        UserDO userDO = new UserDO();
        userDO.setName(name);
        userDO.setPhone(phone);
        return userService.createUser(userDO) > 0;
    }

    @ApiOperation(value = "按用户ID删除用户")
    @PostMapping(value = "/removeById")
    public boolean removeById(@RequestParam(defaultValue = "1") Long id) {
        return userService.removeUserById(id) > 0;
    }

    @ApiOperation(value = "修改用户")
    @PostMapping(value = "/modifyUser")
    public boolean modifyUser(@RequestParam Long id,
                              String name,
                              String phone) {
        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setName(name);
        userDO.setPhone(phone);
        return userService.modifyUser(userDO) > 0;
    }

    @ApiOperation(value = "批量写入一波")
    @PostMapping(value = "/batchGen")
    public boolean batchGen() {
        userService.batchInsert();
        return true;
    }
}
