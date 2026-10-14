package vn.comtruatrachieu.be.group;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import vn.comtruatrachieu.be.auth.CurrentUser;
import vn.comtruatrachieu.be.member.Member;
import vn.comtruatrachieu.be.member.MemberRepository;

/** Nhóm: mỗi người chỉ thấy và sửa được nhóm mình đang ở trong. */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    public record GroupRequest(
            @NotBlank(message = "Tên nhóm không được để trống") @Size(max = 60, message = "Tên nhóm tối đa 60 ký tự") String name,
            @NotNull(message = "Thiếu danh sách thành viên") List<Long> memberIds) {
    }

    private final MemberGroupRepository groups;
    private final MemberRepository members;
    private final CurrentUser currentUser;

    public GroupController(MemberGroupRepository groups, MemberRepository members, CurrentUser currentUser) {
        this.groups = groups;
        this.members = members;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<MemberGroup> list(@AuthenticationPrincipal Jwt jwt) {
        return groups.findAllByMemberId(currentUser.member(jwt).getId());
    }

    /** Người tạo luôn được thêm vào nhóm. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberGroup create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GroupRequest request) {
        Member me = currentUser.member(jwt);
        MemberGroup group = new MemberGroup();
        group.setCreatedAt(Instant.now());
        Set<Long> ids = new HashSet<>(request.memberIds());
        ids.add(me.getId());
        return groups.save(apply(group, request.name(), ids));
    }

    @PutMapping("/{id}")
    public MemberGroup update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        MemberGroup group = findMine(jwt, id);
        Set<Long> ids = new HashSet<>(request.memberIds());
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm phải có ít nhất 1 thành viên");
        }
        return groups.save(apply(group, request.name(), ids));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        groups.delete(findMine(jwt, id));
    }

    private MemberGroup findMine(Jwt jwt, Long id) {
        Long myId = currentUser.member(jwt).getId();
        // nhóm không có mình thì coi như không tồn tại
        return groups.findById(id)
                .filter(g -> g.hasMember(myId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm " + id));
    }

    private MemberGroup apply(MemberGroup group, String name, Set<Long> memberIds) {
        List<Member> found = members.findAllById(memberIds);
        if (found.size() != memberIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có thành viên không còn tồn tại");
        }
        group.setName(name.trim());
        group.getMembers().clear();
        group.getMembers().addAll(found);
        return group;
    }
}
