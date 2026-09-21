package com.example.picturebackend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.example.picturebackend.mapper.SpaceUserMapper;
import com.example.picturebackend.model.entity.SpaceUser;
import com.example.picturebackend.service.SpaceUserService;
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService{

}

