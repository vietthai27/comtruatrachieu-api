package vn.comtruatrachieu.be.member;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.transaction.annotation.Transactional;

import vn.comtruatrachieu.be.auth.UserAccountRepository;
import vn.comtruatrachieu.be.group.MemberGroup;
import vn.comtruatrachieu.be.group.MemberGroupRepository;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    public record MemberRequest(
            @NotBlank(message = "Họ tên không được để trống") String fullName,
            @NotBlank(message = "Chưa chọn ngân hàng") String bankBin,
            @NotBlank(message = "Số tài khoản không được để trống")
            @Pattern(regexp = "\\d{4,30}", message = "Số tài khoản chỉ gồm chữ số") String accountNo) {
    }

    private final MemberRepository repository;
    private final UserAccountRepository accounts;
    private final MemberGroupRepository groups;

    public MemberController(MemberRepository repository, UserAccountRepository accounts, MemberGroupRepository groups) {
        this.repository = repository;
        this.accounts = accounts;
        this.groups = groups;
    }

    @GetMapping
    public List<Member> list() {
        return repository.findAll(Sort.by("fullName"));
    }

    @GetMapping("/{id}")
    public Member get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Member create(@Valid @RequestBody MemberRequest request) {
        return repository.save(apply(new Member(), request));
    }

    @PutMapping("/{id}")
    public Member update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return repository.save(apply(find(id), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    /**
     * Admin xoá thành viên: gỡ khỏi các nhóm, xoá tài khoản đăng nhập (nếu có), rồi xoá thành viên.
     * Lịch sử hoá đơn giữ nguyên vì hoá đơn lưu bản sao tên / số tài khoản, không trỏ khoá ngoại tới thành viên.
     */
    @Transactional
    public void delete(@PathVariable Long id) {
        Member member = find(id);
        for (MemberGroup group : groups.findAllByMemberId(id)) {
            group.getMembers().removeIf(m -> m.getId().equals(id));
            groups.save(group);
        }
        accounts.deleteByMemberId(id);
        repository.delete(member);
    }

    private Member find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên " + id));
    }

    private static Member apply(Member member, MemberRequest request) {
        member.setFullName(request.fullName().trim());
        member.setBankBin(request.bankBin().trim());
        member.setAccountNo(request.accountNo().trim());
        return member;
    }
}
