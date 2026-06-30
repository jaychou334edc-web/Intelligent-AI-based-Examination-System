package com.aes.exam.sample.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.aes.exam.sample.dto.SampleUserCreateRequest;
import com.aes.exam.sample.entity.SampleUserEntity;
import com.aes.exam.sample.vo.SampleUserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SampleUserMapperTest {

    @Autowired
    private SampleUserMapper mapper;

    @Test
    void mapsDtoEntityAndVo() {
        SampleUserCreateRequest request = new SampleUserCreateRequest("教师甲", "teacher@example.com");

        SampleUserEntity entity = mapper.toEntity(request);
        SampleUserVO vo = mapper.toVo(entity);

        assertThat(entity.id()).isNotBlank();
        assertThat(entity.role()).isEqualTo("DIAGNOSTIC_USER");
        assertThat(vo.displayName()).isEqualTo("教师甲");
        assertThat(vo.email()).isEqualTo("teacher@example.com");
    }
}
