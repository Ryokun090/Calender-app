package com.hakkushon.calendarapp.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.PollCandidate;

@Mapper
public interface PollCandidateMapper {

    int insert(PollCandidate candidate);

    Optional<PollCandidate> findById(@Param("id") Long id);

    List<PollCandidate> findByPollId(@Param("pollId") Long pollId);
}
