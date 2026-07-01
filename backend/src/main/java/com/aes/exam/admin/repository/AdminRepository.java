package com.aes.exam.admin.repository;

import com.aes.exam.admin.vo.LoginSessionAuditVO;
import java.util.List;

public interface AdminRepository {

    List<LoginSessionAuditVO> findRecentSessions(int limit);
}
