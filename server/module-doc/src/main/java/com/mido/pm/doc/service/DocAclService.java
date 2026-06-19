package com.mido.pm.doc.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.doc.dto.DocAclGrantDTO;
import com.mido.pm.doc.dto.DocAclVO;
import com.mido.pm.doc.dto.DocShareVO;
import com.mido.pm.doc.dto.PublicDocVO;
import com.mido.pm.doc.entity.PmDoc;
import com.mido.pm.doc.entity.PmDocAcl;
import com.mido.pm.doc.entity.PmDocShare;
import com.mido.pm.doc.entity.PmDocVersion;
import com.mido.pm.doc.mapper.PmDocAclMapper;
import com.mido.pm.doc.mapper.PmDocMapper;
import com.mido.pm.doc.mapper.PmDocShareMapper;
import com.mido.pm.doc.mapper.PmDocVersionMapper;
import com.mido.pm.org.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文档级权限（ACL）解析与鉴权 + 公开分享外链。
 * 权限就近继承：从节点上溯到最近定义 ACL 的祖先，由其授权决定；链上无 ACL 则默认开放（项目成员可读写）。
 * 创建者恒为 admin。无认证上下文（系统/测试）放行。
 */
@Service
public class DocAclService {

    public static final int NONE = 0, READ = 1, WRITE = 2, ADMIN = 3;

    private final PmDocMapper docMapper;
    private final PmDocAclMapper aclMapper;
    private final PmDocShareMapper shareMapper;
    private final PmDocVersionMapper versionMapper;
    private final SysUserService userService;

    public DocAclService(PmDocMapper docMapper, PmDocAclMapper aclMapper, PmDocShareMapper shareMapper,
                         PmDocVersionMapper versionMapper, SysUserService userService) {
        this.docMapper = docMapper;
        this.aclMapper = aclMapper;
        this.shareMapper = shareMapper;
        this.versionMapper = versionMapper;
        this.userService = userService;
    }

    // ===== 解析 =====

    static int rank(String permission) {
        if ("admin".equals(permission)) return ADMIN;
        if ("write".equals(permission)) return WRITE;
        if ("read".equals(permission)) return READ;
        return NONE;
    }

    private Set<String> principalsOf(Long uid) {
        Set<String> p = new HashSet<>();
        p.add(PmDocAcl.P_USER + ":" + uid);
        for (Long rid : userService.roleIdsOf(uid)) {
            p.add(PmDocAcl.P_ROLE + ":" + rid);
        }
        return p;
    }

    /** 沿父链解析有效权限等级。 */
    private int resolveWalk(PmDoc start, Function<Long, PmDoc> docFetcher,
                            Function<Long, List<PmDocAcl>> aclFetcher, Long uid, Set<String> principals) {
        if (uid == null) {
            return ADMIN; // 无认证上下文：放行（真实接口均已鉴权）
        }
        PmDoc cur = start;
        int guard = 0;
        while (cur != null && guard++ < 64) {
            if (uid.equals(cur.getCreateBy())) {
                return ADMIN;
            }
            List<PmDocAcl> entries = aclFetcher.apply(cur.getId());
            if (entries != null && !entries.isEmpty()) {
                int max = NONE;
                for (PmDocAcl a : entries) {
                    if (principals.contains(a.getPrincipalType() + ":" + a.getPrincipalId())) {
                        max = Math.max(max, rank(a.getPermission()));
                    }
                }
                return max; // 就近定义即决定
            }
            Long pid = cur.getParentId() == null ? 0L : cur.getParentId();
            cur = pid == 0L ? null : docFetcher.apply(pid);
        }
        return WRITE; // 链上无 ACL → 默认开放
    }

    private int resolveForDoc(PmDoc d) {
        Long uid = currentUserId();
        return resolveWalk(d, id -> docMapper.selectById(id),
                id -> aclMapper.selectList(Wrappers.<PmDocAcl>lambdaQuery().eq(PmDocAcl::getDocId, id)),
                uid, uid == null ? Set.of() : principalsOf(uid));
    }

    public void requireRead(Long docId) {
        ensure(docId, READ);
    }

    public void requireWrite(Long docId) {
        ensure(docId, WRITE);
    }

    public void requireAdmin(Long docId) {
        ensure(docId, ADMIN);
    }

    private void ensure(Long docId, int need) {
        PmDoc d = docMapper.selectById(docId);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        if (resolveForDoc(d) < need) {
            throw new BizException(ErrorCode.FORBIDDEN, "无该文档权限");
        }
    }

    /** 批量计算项目内可读节点 id（单次加载 ACL，供目录树过滤）。 */
    public Set<Long> readableDocIds(List<PmDoc> all) {
        Long uid = currentUserId();
        if (uid == null || all.isEmpty()) {
            return all.stream().map(PmDoc::getId).collect(Collectors.toSet());
        }
        List<Long> ids = all.stream().map(PmDoc::getId).toList();
        Map<Long, List<PmDocAcl>> aclByDoc = aclMapper.selectList(
                        Wrappers.<PmDocAcl>lambdaQuery().in(PmDocAcl::getDocId, ids))
                .stream().collect(Collectors.groupingBy(PmDocAcl::getDocId));
        Map<Long, PmDoc> byId = all.stream().collect(Collectors.toMap(PmDoc::getId, d -> d));
        Set<String> principals = principalsOf(uid);
        Set<Long> readable = new HashSet<>();
        for (PmDoc d : all) {
            int r = resolveWalk(d, byId::get, id -> aclByDoc.getOrDefault(id, List.of()), uid, principals);
            if (r >= READ) {
                readable.add(d.getId());
            }
        }
        return readable;
    }

    // ===== ACL 管理 =====

    public List<DocAclVO> listAcl(Long docId) {
        requireAdmin(docId);
        return aclMapper.selectList(Wrappers.<PmDocAcl>lambdaQuery().eq(PmDocAcl::getDocId, docId))
                .stream().map(a -> new DocAclVO(a.getId(), a.getPrincipalType(), a.getPrincipalId(), a.getPermission()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long grant(Long docId, DocAclGrantDTO dto) {
        requireAdmin(docId);
        if (rank(dto.permission()) == NONE
                || (!PmDocAcl.P_USER.equals(dto.principalType()) && !PmDocAcl.P_ROLE.equals(dto.principalType()))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法的授权参数");
        }
        // 同一主体覆盖：先清后插
        aclMapper.delete(Wrappers.<PmDocAcl>lambdaQuery().eq(PmDocAcl::getDocId, docId)
                .eq(PmDocAcl::getPrincipalType, dto.principalType()).eq(PmDocAcl::getPrincipalId, dto.principalId()));
        PmDocAcl a = new PmDocAcl();
        a.setDocId(docId);
        a.setPrincipalType(dto.principalType());
        a.setPrincipalId(dto.principalId());
        a.setPermission(dto.permission());
        a.setCreateBy(currentUserId());
        a.setUpdateBy(currentUserId());
        aclMapper.insert(a);
        return a.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long aclId) {
        PmDocAcl a = aclMapper.selectById(aclId);
        if (a == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "授权不存在");
        }
        requireAdmin(a.getDocId());
        aclMapper.deleteById(aclId);
    }

    // ===== 公开分享 =====

    /** 创建/复用分享外链（admin）。expireTime 可空=永久。 */
    @Transactional(rollbackFor = Exception.class)
    public DocShareVO createShare(Long docId, LocalDateTime expireTime) {
        requireAdmin(docId);
        PmDocShare s = shareMapper.selectOne(Wrappers.<PmDocShare>lambdaQuery()
                .eq(PmDocShare::getDocId, docId).last("limit 1"));
        if (s == null) {
            s = new PmDocShare();
            s.setDocId(docId);
            s.setToken(UUID.randomUUID().toString().replace("-", ""));
            s.setExpireTime(expireTime);
            s.setEnabled(1);
            s.setCreateBy(currentUserId());
            s.setUpdateBy(currentUserId());
            shareMapper.insert(s);
        } else {
            s.setEnabled(1);
            s.setExpireTime(expireTime);
            s.setUpdateBy(currentUserId());
            shareMapper.updateById(s);
        }
        return toShareVO(s);
    }

    /** 当前分享（admin 查看），无则返回 null。 */
    public DocShareVO getShare(Long docId) {
        requireAdmin(docId);
        PmDocShare s = shareMapper.selectOne(Wrappers.<PmDocShare>lambdaQuery()
                .eq(PmDocShare::getDocId, docId).last("limit 1"));
        return s == null ? null : toShareVO(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableShare(Long docId) {
        requireAdmin(docId);
        PmDocShare s = shareMapper.selectOne(Wrappers.<PmDocShare>lambdaQuery()
                .eq(PmDocShare::getDocId, docId).last("limit 1"));
        if (s != null) {
            s.setEnabled(0);
            s.setUpdateBy(currentUserId());
            shareMapper.updateById(s);
        }
    }

    /** 匿名按 token 读取（公开只读，无鉴权）。校验启用与过期。 */
    public PublicDocVO getByToken(String token) {
        PmDocShare s = shareMapper.selectOne(Wrappers.<PmDocShare>lambdaQuery()
                .eq(PmDocShare::getToken, token).last("limit 1"));
        if (s == null || !Integer.valueOf(1).equals(s.getEnabled())
                || (s.getExpireTime() != null && s.getExpireTime().isBefore(LocalDateTime.now()))) {
            throw new BizException(ErrorCode.NOT_FOUND, "分享链接无效或已过期");
        }
        PmDoc d = docMapper.selectById(s.getDocId());
        if (d == null || Integer.valueOf(1).equals(d.getTrashed())) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在");
        }
        PmDocVersion v = d.getCurrentVersionId() == null ? null : versionMapper.selectById(d.getCurrentVersionId());
        return new PublicDocVO(d.getTitle(), v == null ? null : v.getContent());
    }

    private DocShareVO toShareVO(PmDocShare s) {
        return new DocShareVO(s.getId(), s.getDocId(), s.getToken(), s.getExpireTime(), s.getEnabled());
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }
}
