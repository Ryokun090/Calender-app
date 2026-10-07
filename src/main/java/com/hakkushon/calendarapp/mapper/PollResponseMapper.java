package com.hakkushon.calendarapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.PollResponse;

@Mapper
public interface PollResponseMapper {

    /** candidate_id+user_idの組で既にあれば更新、無ければ新規作成する(ON DUPLICATE KEY UPDATE)。 */
    int upsert(@Param("candidateId") Long candidateId,
               @Param("userId") Long userId,
               @Param("response") String response);

    List<PollResponse> findByCandidateIds(@Param("candidateIds") List<Long> candidateIds);
}
