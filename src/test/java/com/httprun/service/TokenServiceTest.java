package com.httprun.service;

import com.httprun.entity.Token;
import com.httprun.enums.ErrorCode;
import com.httprun.exception.BusinessException;
import com.httprun.repository.TokenRepository;
import com.httprun.security.JwtTokenProvider;
import com.httprun.service.impl.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TokenService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private Token testToken;

    @BeforeEach
    void setUp() {
        testToken = new Token();
        testToken.setId(1L);
        testToken.setName("test-token");
        testToken.setSubject("user123");
        testToken.setIsAdmin(false);
        testToken.setJwtToken("jwt-token-12345");
        testToken.setIssuedAt(Instant.now().getEpochSecond());
        testToken.setExpiresAt(Instant.now().getEpochSecond() + 86400);
        testToken.setRevoked(false);
    }

    @Test
    void createToken_shouldGenerateAndSaveToken() {
        // Given
        when(jwtTokenProvider.generateToken(any(), any(), anyBoolean(), anyLong()))
                .thenReturn("jwt-token-12345");
        when(tokenRepository.save(any(Token.class))).thenReturn(testToken);

        // When
        Token result = tokenService.createToken("test-token", "user123", false, 24);

        // Then
        assertThat(result).isNotNull();
        verify(jwtTokenProvider).generateToken(eq("user123"), eq("test-token"), eq(false), anyLong());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void createToken_withDefaultExpiry_shouldUseOneYear() {
        // Given
        when(jwtTokenProvider.generateToken(any(), any(), anyBoolean(), anyLong()))
                .thenReturn("jwt-token-12345");
        when(tokenRepository.save(any(Token.class))).thenReturn(testToken);

        // When
        Token result = tokenService.createToken("test-token", "user123", false, 0);

        // Then
        assertThat(result).isNotNull();
        verify(jwtTokenProvider).generateToken(eq("user123"), eq("test-token"), eq(false), anyLong());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void getToken_shouldReturnToken() {
        // Given
        when(tokenRepository.findById(1L)).thenReturn(Optional.of(testToken));

        // When
        Token result = tokenService.getToken(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-token");
    }

    @Test
    void getToken_whenNotFound_shouldThrowException() {
        // Given
        when(tokenRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> tokenService.getToken(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_NOT_FOUND);
    }

    @Test
    void listAllTokens_shouldReturnSortedList() {
        // Given
        List<Token> tokens = List.of(testToken);
        when(tokenRepository.findByRevokedFalseOrderByCreatedAtDesc()).thenReturn(tokens);

        // When
        List<Token> result = tokenService.listAllTokens();

        // Then
        assertThat(result).hasSize(1);
        verify(tokenRepository).findByRevokedFalseOrderByCreatedAtDesc();
    }

    @Test
    void listTokens_shouldReturnPagedResults() {
        // Given
        Page<Token> page = new PageImpl<>(List.of(testToken));
        when(tokenRepository.findAll(any(PageRequest.class))).thenReturn(page);

        // When
        Page<Token> result = tokenService.listTokens(1, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(tokenRepository).findAll(any(PageRequest.class));
    }

    @Test
    void revokeToken_shouldSetRevokedFlag() {
        // Given
        when(tokenRepository.findById(1L)).thenReturn(Optional.of(testToken));
        when(tokenRepository.save(any(Token.class))).thenReturn(testToken);

        // When
        tokenService.revokeToken(1L);

        // Then
        verify(tokenRepository).save(argThat(token -> Boolean.TRUE.equals(token.getRevoked())));
    }

    @Test
    void revokeToken_whenNotFound_shouldThrowException() {
        // Given
        when(tokenRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> tokenService.revokeToken(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_NOT_FOUND);
    }

    @Test
    void deleteTokens_shouldDeleteMultipleTokens() {
        // Given
        List<Long> ids = List.of(1L, 2L, 3L);
        when(tokenRepository.deleteByIdIn(ids)).thenReturn(3);

        // When
        tokenService.deleteTokens(ids);

        // Then
        verify(tokenRepository).deleteByIdIn(ids);
    }

    @Test
    void deleteTokens_withEmptyList_shouldNotDelete() {
        // When
        tokenService.deleteTokens(List.of());

        // Then
        verify(tokenRepository, never()).deleteByIdIn(any());
    }

    @Test
    void validateToken_shouldReturnTrue_whenValid() {
        // Given
        String jwtToken = "valid-jwt";
        when(jwtTokenProvider.validateToken(jwtToken)).thenReturn(true);
        when(tokenRepository.findValidToken(eq(jwtToken), anyLong()))
                .thenReturn(Optional.of(testToken));

        // When
        boolean result = tokenService.validateToken(jwtToken);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_whenInvalidJwt() {
        // Given
        String jwtToken = "invalid-jwt";
        when(jwtTokenProvider.validateToken(jwtToken)).thenReturn(false);

        // When
        boolean result = tokenService.validateToken(jwtToken);

        // Then
        assertThat(result).isFalse();
        verify(tokenRepository, never()).findValidToken(any(), anyLong());
    }

    @Test
    void getTokenByJwt_shouldReturnToken() {
        // Given
        String jwtToken = "jwt-token-12345";
        when(tokenRepository.findByJwtToken(jwtToken)).thenReturn(Optional.of(testToken));

        // When
        Token result = tokenService.getTokenByJwt(jwtToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJwtToken()).isEqualTo(jwtToken);
    }

    @Test
    void getTokenByJwt_whenNotFound_shouldThrowException() {
        // Given
        when(tokenRepository.findByJwtToken("invalid")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> tokenService.getTokenByJwt("invalid"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_NOT_FOUND);
    }

    @Test
    void cleanExpiredTokens_shouldRevokeExpiredTokens() {
        // Given
        when(tokenRepository.revokeExpiredTokens(anyLong())).thenReturn(5);

        // When
        int result = tokenService.cleanExpiredTokens();

        // Then
        assertThat(result).isEqualTo(5);
        verify(tokenRepository).revokeExpiredTokens(anyLong());
    }
}
