# Security Experiments & Attack Simulations

This directory contains security experiment scripts to validate the cryptographic protections and security mechanisms of the e-commerce platform.

## Experiments Overview

According to YEUCAU.md Section 8.2, we need to conduct 5 key security experiments:

1. **Token Replay & Binding** - Test token security and device binding
2. **Payment Fraud Simulation** - Simulate payment attacks and test fraud detection
3. **API Abuse & Rate Limiting** - Test rate limiting and abuse prevention
4. **Key Compromise & Rotation** - Test key rotation and recovery procedures
5. **Supply Chain Tamper** - Validate artifact verification

## Prerequisites

```bash
# Install Python dependencies
pip install requests pytest faker

# Install Node.js dependencies for load testing
npm install -g artillery k6
```

## Running Experiments

### Experiment 1: Token Replay Attack

Tests whether stolen JWT tokens can be replayed from different devices.

```bash
cd token-replay-test
python test_token_replay.py
```

**Expected Results:**
- ✅ Original device can access protected resources
- ❌ Replayed token from different device/IP is rejected
- ✅ Refresh token rotation prevents reuse

### Experiment 2: Payment Fraud Detection

Simulates various fraud scenarios to test detection mechanisms.

```bash
cd payment-fraud-test
python test_fraud_detection.py
```

**Test Scenarios:**
- Multiple failed payment attempts
- Unusually high transaction amounts
- Rapid successive orders from same account
- Suspicious shipping address changes
- Geolocation mismatches

**Expected Results:**
- Risk score calculated correctly
- High-risk transactions flagged or blocked
- Audit logs created for all suspicious activities

### Experiment 3: API Rate Limiting & Abuse

Tests API protection against automated abuse.

```bash
cd api-abuse-test
python test_rate_limiting.py
```

**Test Scenarios:**
- Credential stuffing (multiple login attempts)
- Excessive cart updates
- Rapid product queries
- Order submission spam

**Expected Results:**
- Rate limiter blocks requests after threshold
- HTTP 429 (Too Many Requests) returned
- Exponential backoff enforced

### Experiment 4: Key Rotation Drill

Tests key management and rotation procedures.

```bash
cd key-rotation-drill
./rotate_keys.sh
```

**Steps:**
1. Generate new encryption key in Vault
2. Re-encrypt existing data with new key
3. Update service configurations
4. Verify old key is revoked
5. Validate all services still functional

### Experiment 5: HMAC Signature Verification

Tests service-to-service authentication and replay protection.

```bash
cd hmac-verification-test
python test_hmac_signatures.py
```

**Test Scenarios:**
- Valid HMAC signature accepted
- Modified request body rejected
- Expired timestamp rejected
- Replay with old signature rejected

## Load Testing

### Using Artillery

```bash
cd load-tests
artillery run catalog-load-test.yml
artillery run checkout-load-test.yml
```

### Using K6

```bash
k6 run --vus 50 --duration 30s load-test-catalog.js
k6 run --vus 20 --duration 60s load-test-checkout.js
```

## Performance Benchmarks

Expected baseline performance metrics:

| Endpoint | p50 Latency | p95 Latency | p99 Latency | Throughput |
|----------|-------------|-------------|-------------|------------|
| GET /catalog/products | <50ms | <100ms | <200ms | 1000 req/s |
| POST /cart/items | <100ms | <200ms | <400ms | 500 req/s |
| POST /orders | <300ms | <600ms | <1000ms | 100 req/s |
| POST /payments | <500ms | <1000ms | <2000ms | 50 req/s |

## Security Metrics

Track the following security KPIs:

- **Token Replay Success Rate**: Should be 0%
- **Fraud Detection Accuracy**: >95% (low false positive rate)
- **Rate Limit Effectiveness**: >99% of abuse blocked
- **Key Rotation MTTR**: <5 minutes
- **HMAC Verification Pass Rate**: 100% for valid, 0% for tampered

## Reporting

Results are stored in:
- `results/` - Raw test outputs (JSON/CSV)
- `reports/` - Generated HTML reports
- `dashboards/` - Grafana dashboard configs

## Continuous Testing

Integrate experiments into CI/CD:

```yaml
# .github/workflows/security-tests.yml
name: Security Experiments
on: [push, pull_request]
jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Token Replay Test
        run: python experiments/token-replay-test/test_token_replay.py
      - name: Run Fraud Detection Test
        run: python experiments/payment-fraud-test/test_fraud_detection.py
```

## Notes

- All tests run in **isolated test environment only**
- Never run against production systems
- Use synthetic test data only
- Follow responsible disclosure for any vulnerabilities found
