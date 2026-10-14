package vn.comtruatrachieu.be.bill;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BillRepository extends JpaRepository<Bill, Long> {

    /** Hoá đơn có thành viên này tham gia (người trả cũng nằm trong danh sách tham gia), mới nhất trước. */
    @Query("select distinct b from Bill b join b.participants p where p.memberId = :memberId order by b.createdAt desc")
    List<Bill> findAllByParticipant(Long memberId);

    @Query("select distinct b from Bill b join b.participants p where b.id = :id and p.memberId = :memberId")
    Optional<Bill> findByIdAndParticipant(Long id, Long memberId);
}
