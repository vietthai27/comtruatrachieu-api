package vn.comtruatrachieu.be.bill;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

@Entity
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private long totalAmount;

    /** Số tiền mỗi người (đã làm tròn lên 1.000đ). */
    @Column(nullable = false)
    private long shareAmount;

    private String note;

    // Nhóm đã chọn khi chia (bản sao tên; null nếu chia theo "Tất cả")
    private Long groupId;

    private String groupName;

    // Bản sao thông tin người trả tại thời điểm lưu
    private Long payerMemberId;

    @Column(nullable = false)
    private String payerName;

    @Column(nullable = false)
    private String payerBankBin;

    @Column(nullable = false)
    private String payerAccountNo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bill_participant", joinColumns = @JoinColumn(name = "bill_id"))
    @OrderColumn(name = "sort_order")
    private List<BillParticipant> participants = new ArrayList<>();

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public long getShareAmount() { return shareAmount; }
    public void setShareAmount(long shareAmount) { this.shareAmount = shareAmount; }
    public String getNote() { return note; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setNote(String note) { this.note = note; }
    public Long getPayerMemberId() { return payerMemberId; }
    public void setPayerMemberId(Long payerMemberId) { this.payerMemberId = payerMemberId; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getPayerBankBin() { return payerBankBin; }
    public void setPayerBankBin(String payerBankBin) { this.payerBankBin = payerBankBin; }
    public String getPayerAccountNo() { return payerAccountNo; }
    public void setPayerAccountNo(String payerAccountNo) { this.payerAccountNo = payerAccountNo; }
    public List<BillParticipant> getParticipants() { return participants; }
}
