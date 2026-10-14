package vn.comtruatrachieu.be.group;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberGroupRepository extends JpaRepository<MemberGroup, Long> {

    /** Các nhóm có thành viên này, sắp theo tên. */
    @Query("select distinct g from MemberGroup g join g.members m where m.id = :memberId order by g.name")
    List<MemberGroup> findAllByMemberId(Long memberId);
}
