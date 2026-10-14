package vn.comtruatrachieu.be.bill;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import vn.comtruatrachieu.be.auth.CurrentUser;
import vn.comtruatrachieu.be.group.MemberGroupRepository;
import vn.comtruatrachieu.be.member.Member;
import vn.comtruatrachieu.be.member.MemberRepository;

/** Hoá đơn: mỗi người chỉ xem được hoá đơn mà mình có tham gia. */
@RestController
@RequestMapping("/api/bills")
public class BillController {

    public record BillRequest(
            @NotNull(message = "Thiếu tổng tiền") @Positive(message = "Tổng tiền phải lớn hơn 0") Long totalAmount,
            String note,
            @NotNull(message = "Chưa chọn người trả") Long payerId,
            @NotNull @Size(min = 2, message = "Cần ít nhất 2 người tham gia") List<Long> participantIds,
            Long groupId) {
    }

    private final BillRepository bills;
    private final MemberRepository members;
    private final MemberGroupRepository groups;
    private final CurrentUser currentUser;

    public BillController(BillRepository bills, MemberRepository members, MemberGroupRepository groups,
                          CurrentUser currentUser) {
        this.bills = bills;
        this.members = members;
        this.groups = groups;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Bill> list(@AuthenticationPrincipal Jwt jwt) {
        return bills.findAllByParticipant(currentUser.member(jwt).getId());
    }

    @GetMapping("/{id}")
    public Bill get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        // hoá đơn không có mình thì coi như không tồn tại
        return bills.findByIdAndParticipant(id, currentUser.member(jwt).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hoá đơn " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Bill create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BillRequest request) {
        var ids = new LinkedHashSet<>(request.participantIds());
        if (ids.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ít nhất 2 người tham gia");
        }
        if (!ids.contains(request.payerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người trả phải nằm trong danh sách tham gia");
        }
        Map<Long, Member> byId = members.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        if (byId.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có thành viên không còn tồn tại");
        }

        Member payer = byId.get(request.payerId());
        Bill bill = new Bill();
        bill.setCreatedAt(Instant.now());
        bill.setTotalAmount(request.totalAmount());
        bill.setShareAmount(roundUpThousand(request.totalAmount(), ids.size()));
        bill.setNote(request.note() == null ? null : request.note().trim());
        bill.setPayerMemberId(payer.getId());
        bill.setPayerName(payer.getFullName());
        bill.setPayerBankBin(payer.getBankBin());
        bill.setPayerAccountNo(payer.getAccountNo());
        if (request.groupId() != null) {
            Long myId = currentUser.member(jwt).getId();
            groups.findById(request.groupId())
                    .filter(g -> g.hasMember(myId))
                    .ifPresent(g -> {
                        bill.setGroupId(g.getId());
                        bill.setGroupName(g.getName());
                    });
        }
        ids.forEach(id -> bill.getParticipants().add(new BillParticipant(id, byId.get(id).getFullName())));
        return bills.save(bill);
    }

    /** Giống FE: chia đều rồi làm tròn lên 1.000đ. */
    private static long roundUpThousand(long total, int people) {
        long perPerson = (total + people - 1) / people;
        return (perPerson + 999) / 1000 * 1000;
    }
}
