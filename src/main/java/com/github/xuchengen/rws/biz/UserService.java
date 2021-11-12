package com.github.xuchengen.rws.biz;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xuchengen.rws.dao.mapper.UserDOMapper;
import com.github.xuchengen.rws.dao.model.UserDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class UserService {

    @Resource(name = "userDOMapper")
    private UserDOMapper userDOMapper;

    public Integer createUser(UserDO userDO) {
        return userDOMapper.insertSelective(userDO);
    }

    public Integer removeUserById(Long id) {
        return userDOMapper.deleteByPrimaryKey(id);
    }

    public Integer modifyUser(UserDO userDO) {
        return userDOMapper.updateByPrimaryKeySelective(userDO);
    }

    public UserDO getById(Long id) {
        return userDOMapper.selectByPrimaryKey(id);
    }

    public PageInfo<UserDO> queryByPage(Integer pageNum, Integer pageSize) {
        return PageHelper.startPage(pageNum, pageSize)
                .doSelectPageInfo(() -> userDOMapper.selectAll());
    }

    @Transactional
    public void batchInsert() {
        String name = "徐承恩";
        String phone = "13111111111";
        for (int i = 0; i < 100; i++) {
            UserDO userDO = new UserDO();
            userDO.setName(name);
            userDO.setPhone(phone);
            userDOMapper.insertSelective(userDO);
        }
    }
}
