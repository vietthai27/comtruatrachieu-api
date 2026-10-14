package vn.comtruatrachieu.be.bill;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Người tham gia hoá đơn — lưu bản sao tên để lịch sử không đổi khi sửa/xoá thành viên. */
@Embeddable
public class BillParticipant {

    private Long memberId;

    @Column(nullable = false)
    private String fullName;

    protected BillParticipant() {
    }

    public BillParticipant(Long memberId, String fullName) {
        this.memberId = memberId;
        this.fullName = fullName;
    }

    public Long getMemberId() { return memberId; }
    public String getFullName() { return fullName; }
}
