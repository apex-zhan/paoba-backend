package com.zxw.paoba.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxw.paoba.mapper.TagMapper;
import com.zxw.paoba.model.domain.Tag;
import com.zxw.paoba.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author MECHREVO
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-08-12 18:46:48
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService {

}




