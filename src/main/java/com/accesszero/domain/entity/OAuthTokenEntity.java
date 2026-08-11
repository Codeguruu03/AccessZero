package com.accesszero.domain.entity;

import com.accesszero.domain.enums.TokenType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_tokens")
public class OAuthTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "scopes")
    private String scopes;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public OAuthTokenEntity() {}

    public OAuthTokenEntity(Long userId, TokenType tokenType, String clientId, String tokenHash, String scopes, boolean revoked, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenType = tokenType;
        this.clientId = clientId;
        this.tokenHash = tokenHash;
        this.scopes = scopes;
        this.revoked = revoked;
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public TokenType getTokenType() { return tokenType; }
    public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
