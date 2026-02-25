package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.models.HouseCreateResultEntity;
import com.rafex.housedb.repository.models.HouseMemberEntity;
import com.rafex.housedb.repository.models.HouseSummaryEntity;
import com.rafex.housedb.service.models.HouseCreateResult;
import com.rafex.housedb.service.models.HouseMember;
import com.rafex.housedb.service.models.HouseSummary;

import java.util.List;

final class HouseModelMapper {

    HouseCreateResult toHouseCreateResult(final HouseCreateResultEntity source) {
        return new HouseCreateResult(source.houseId(), source.houseMemberId());
    }

    HouseMember toHouseMember(final HouseMemberEntity source) {
        return new HouseMember(source.houseMemberId(), source.houseId(), source.userId(), source.role(),
                source.enabled());
    }

    List<HouseSummary> toHouseSummaries(final List<HouseSummaryEntity> source) {
        return source.stream().map(this::toHouseSummary).toList();
    }

    private HouseSummary toHouseSummary(final HouseSummaryEntity source) {
        return new HouseSummary(source.houseId(), source.name(), source.description(), source.city(), source.state(),
                source.country(), source.role(), source.memberEnabled(), source.houseEnabled());
    }

    List<HouseMember> toHouseMembers(final List<HouseMemberEntity> source) {
        return source.stream().map(this::toHouseMember).toList();
    }
}
