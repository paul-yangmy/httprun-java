package com.httprun.security;

import com.httprun.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现
 * 
 * 本系统主要使用 JWT Token 认证，此类用于支持 Spring Security 的 UserDetailsService 接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final TokenRepository tokenRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 在 JWT Token 认证模式下，通过 Token 名称查找
        var tokens = tokenRepository.findByName(username);

        if (tokens.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        var token = tokens.get(0);

        // 检查 Token 是否被撤销
        if (token.getRevoked()) {
            throw new UsernameNotFoundException("Token has been revoked: " + username);
        }

        // 构建权限
        var authority = token.getIsAdmin()
                ? new SimpleGrantedAuthority("ROLE_ADMIN")
                : new SimpleGrantedAuthority("ROLE_USER");

        return User.builder()
                .username(token.getName())
                .password("") // JWT 模式不需要密码
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(token.getRevoked())
                .credentialsExpired(false)
                .disabled(token.getRevoked())
                .build();
    }
}
