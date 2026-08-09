# Auth Security Negative Tests

## Registration Tests

### ❌ Weak Password
```bash
POST /api/v1/auth/register
{
  "email": "test@example.com",
  "password": "weak"
}
# Expected: 400 WEAK_PASSWORD
# Reason: Password doesn't meet policy (8+ chars, uppercase, lowercase, digit)
```

### ❌ Duplicate Email
```bash
POST /api/v1/auth/register
{
  "email": "existing@example.com",
  "password": "StrongPass123"
}
# Expected: 409 EMAIL_EXISTS
# Reason: Email already registered
```

### ❌ Invalid Email Format
```bash
POST /api/v1/auth/register
{
  "email": "not-an-email",
  "password": "StrongPass123"
}
# Expected: 400 VALIDATION_ERROR
# Reason: Email fails validation
```

### ❌ Missing Email
```bash
POST /api/v1/auth/register
{
  "password": "StrongPass123"
}
# Expected: 400 VALIDATION_ERROR
# Reason: Email is required
```

## Verify Email Tests

### ❌ Invalid OTP Code
```bash
POST /api/v1/auth/verify-email
{
  "email": "test@example.com",
  "code": "000000"
}
# Expected: 401 INVALID_CODE
# Reason: Code doesn't match hash
```

### ❌ Too Many Attempts (>5)
```bash
# Send 5+ requests with wrong codes
POST /api/v1/auth/verify-email
{
  "email": "test@example.com",
  "code": "000000"
}
# Expected: 429 TOO_MANY_ATTEMPTS after 5th attempt
# Reason: Rate limiting on OTP verification
```

### ❌ Expired OTP Token
```bash
# Wait 15+ minutes after registration
POST /api/v1/auth/verify-email
{
  "email": "test@example.com",
  "code": "123456"
}
# Expected: 401 NO_VALID_TOKEN
# Reason: Token expired (15 min TTL)
```

## Login Tests

### ❌ Wrong Password
```bash
POST /api/v1/auth/login
{
  "email": "test@example.com",
  "password": "WrongPass123"
}
# Expected: 401 INVALID_CREDENTIALS
# Reason: Password doesn't match
```

### ❌ Non-existent User
```bash
POST /api/v1/auth/login
{
  "email": "nonexistent@example.com",
  "password": "StrongPass123"
}
# Expected: 401 INVALID_CREDENTIALS
# Reason: Generic response (anti-enumeration)
```

### ❌ Email Not Verified
```bash
# Register but don't verify email
POST /api/v1/auth/login
{
  "email": "unverified@example.com",
  "password": "StrongPass123"
}
# Expected: 403 EMAIL_NOT_VERIFIED
# Reason: User exists but email not verified
```

### ❌ Inactive Account
```bash
# (Admin deactivates user)
POST /api/v1/auth/login
{
  "email": "deactivated@example.com",
  "password": "StrongPass123"
}
# Expected: 403 ACCOUNT_INACTIVE
# Reason: Account is_active = false
```

### ❌ Brute Force (>5 attempts/15min)
```bash
# Send 6+ login attempts in 15 minutes
POST /api/v1/auth/login
{
  "email": "test@example.com",
  "password": "WrongPass123"
}
# Expected: 429 RATE_LIMIT_EXCEEDED after 5th attempt
# Reason: Rate limiting
```

## Password Reset Tests

### ❌ Non-existent Email
```bash
POST /api/v1/auth/forgot-password
{
  "email": "nonexistent@example.com"
}
# Expected: 200 (generic response)
# Reason: Anti-enumeration - never reveal if email exists
```

### ❌ Invalid Reset Token
```bash
POST /api/v1/auth/reset-password
{
  "token": "invalid-token-string",
  "newPassword": "NewPass123"
}
# Expected: 401 INVALID_TOKEN
# Reason: Token hash doesn't match or expired
```

### ❌ Expired Reset Token
```bash
# Wait 1+ hour after generating token
POST /api/v1/auth/reset-password
{
  "token": "valid-but-expired-token",
  "newPassword": "NewPass123"
}
# Expected: 401 INVALID_TOKEN
# Reason: Token expired (1 hour TTL)
```

### ❌ Weak New Password
```bash
POST /api/v1/auth/reset-password
{
  "token": "valid-token",
  "newPassword": "weak"
}
# Expected: 400 WEAK_PASSWORD
# Reason: New password doesn't meet policy
```

### ❌ Reuse Old Password
```bash
# Note: System doesn't prevent this yet - enhancement for FASE X
POST /api/v1/auth/reset-password
{
  "token": "valid-token",
  "newPassword": "OldPass123"  # Same as current
}
# Expected: 200 (allows for now)
# Reason: Not implemented yet
```

### ❌ Used Token (Already Reset)
```bash
# Token marked as used after first reset
POST /api/v1/auth/reset-password
{
  "token": "already-used-token",
  "newPassword": "AnotherPass123"
}
# Expected: 401 INVALID_TOKEN
# Reason: Token is_used = true
```

## Role Assignment Tests

### ❌ Self-Assign Admin
```bash
POST /api/v1/auth/choose-role
{
  "role": "admin"
}
# Expected: 400 CANNOT_ASSIGN_ADMIN
# Reason: Only backend/admin can assign admin role
# Authentication: Required (authenticated user)
```

### ❌ Assign Role Twice
```bash
# First assignment succeeds
POST /api/v1/auth/choose-role
{
  "role": "chofer"
}
# Response: 200

# Second assignment attempt
POST /api/v1/auth/choose-role
{
  "role": "usuario"
}
# Expected: 400 ROLE_ALREADY_ASSIGNED
# Reason: Role can only be assigned once (from default 'usuario')
```

### ❌ Unauthorized Role Assignment (no auth)
```bash
POST /api/v1/auth/choose-role
{
  "role": "chofer"
}
# Expected: 401 NOT_AUTHENTICATED
# Reason: No session cookie
```

## Session & Cookie Tests

### ❌ Invalid Session Cookie
```bash
GET /api/v1/auth/me
Cookie: nexvia_session=invalid-session-id
# Expected: 401 SESSION_INVALID
# Reason: Session ID not found in Redis/DB
```

### ❌ Expired Session
```bash
# Wait 7+ days
GET /api/v1/auth/me
Cookie: nexvia_session=valid-but-expired-id
# Expected: 401 SESSION_INVALID
# Reason: Session expires_at > NOW() check fails
```

### ❌ Missing Session Cookie
```bash
GET /api/v1/auth/me
# (no Cookie header)
# Expected: 401 NOT_AUTHENTICATED
# Reason: sessionId is undefined
```

### ❌ Tampered Cookie
```bash
# Manually edit cookie value in browser
GET /api/v1/auth/me
Cookie: nexvia_session=tampered-value-xyz
# Expected: 401 SESSION_INVALID
# Reason: Tampered value doesn't match stored hash
```

## Logout Tests

### ❌ Logout Without Auth
```bash
POST /api/v1/auth/logout
# (no session cookie)
# Expected: 401 NOT_AUTHENTICATED
# Reason: authMiddleware requires valid session
```

### ❌ Logout Twice
```bash
# First logout succeeds
POST /api/v1/auth/logout
# Response: 200, cookie cleared

# Second logout attempt with same (now invalid) sessionId
POST /api/v1/auth/logout
# Expected: 401 SESSION_INVALID
# Reason: First logout destroyed the session
```

## Audit Log Verification

### ✅ Login Logged
```sql
SELECT * FROM audit_log 
WHERE action = 'login' AND user_id = '<user_id>'
ORDER BY created_at DESC LIMIT 1;
# Expected: Row exists with action='login', created_at recent
```

### ✅ Password Change Logged
```sql
SELECT * FROM audit_log 
WHERE action = 'password_change' AND user_id = '<user_id>'
ORDER BY created_at DESC LIMIT 1;
# Expected: Row exists after password reset
```

### ✅ Role Change Logged
```sql
SELECT * FROM audit_log 
WHERE action = 'role_change' AND user_id = '<user_id>'
ORDER BY created_at DESC LIMIT 1;
# Expected: Row exists with details = {"from": "usuario", "to": "chofer"}
```

### ✅ Never Log Passwords
```sql
SELECT * FROM audit_log 
WHERE action IN ('login', 'password_change')
AND details LIKE '%password%';
# Expected: 0 rows
# Reason: Passwords should never appear in audit log
```

## API Response Format Tests

### ✅ Error Response Format
```json
{
  "error": {
    "message": "string",
    "code": "string",
    "statusCode": number
  }
}
```

### ✅ Success Response Format (User included)
```json
{
  "message": "string",
  "user": {
    "id": "uuid",
    "email": "string",
    "role": "admin|chofer|usuario",
    "created_at": "ISO8601",
    // NO password_hash included
  }
}
```

### ✅ No Sensitive Data in Responses
- ✅ password_hash never in response
- ✅ google_id never exposed to other users
- ✅ phone/cuit exposed only to self or admin
- ✅ Timestamps in ISO8601 format

## Performance & Limits

### ❌ DoS via OTP Spam
```bash
# 10+ register + verify attempts in 1 minute
POST /api/v1/auth/register  (x10)
# Expected: 429 after rate limit
# Reason: registerRateLimit (3 per hour)
```

### ❌ DoS via Login Spam
```bash
# 10+ login attempts in 1 minute
POST /api/v1/auth/login  (x10)
# Expected: 429 after rate limit
# Reason: loginRateLimit (5 per 15 min)
```

### ❌ Large Payload
```bash
POST /api/v1/auth/register
Content: 11MB
# Expected: 413 PAYLOAD_TOO_LARGE
# Reason: limit '10mb' in middleware
```

---

## Test Execution Checklist

- [ ] All 40+ negative tests executed
- [ ] All expected status codes received
- [ ] Error codes match specification
- [ ] Passwords never logged or returned
- [ ] Audit trail complete
- [ ] Rate limiting working
- [ ] Sessions properly created/destroyed
- [ ] Cookies set with correct flags (HttpOnly, Secure, SameSite)
- [ ] Anti-enumeration patterns verified
- [ ] No information disclosure
