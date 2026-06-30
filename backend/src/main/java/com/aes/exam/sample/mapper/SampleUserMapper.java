package com.aes.exam.sample.mapper;

import com.aes.exam.sample.dto.SampleUserCreateRequest;
import com.aes.exam.sample.entity.SampleUserEntity;
import com.aes.exam.sample.vo.SampleUserVO;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface SampleUserMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "role", constant = "DIAGNOSTIC_USER")
    SampleUserEntity toEntity(SampleUserCreateRequest request);

    SampleUserVO toVo(SampleUserEntity entity);
}
