"""
Token Replay Attack Test
Tests whether JWT tokens can be replayed from different devices/contexts.
"""

import requests
import time
import json
from datetime import datetime

API_BASE = "http://localhost:8080"

class TokenReplayTest:
    def __init__(self):
        self.results = {
            "timestamp": datetime.now().isoformat(),
            "tests": []
        }

    def test_valid_login(self):
        """Test 1: Normal login should succeed"""
        print("\n[TEST 1] Testing valid login...")
        
        response = requests.post(
            f"{API_BASE}/api/catalog/users/login",
            json={
                "username": "user1",
                "password": "8ff8ef3d62115854978812349a72122df63c89ce2fbad21405de7f38f66b2c9027800a09fd7d9ecd5b8596ae24a963915a978366d86a7836e02ed0445543a3f8"
            }
        )
        
        result = {
            "test": "valid_login",
            "status": response.status_code,
            "success": response.status_code == 200
        }
        
        if response.status_code == 200:
            data = response.json()
            token = data.get("token")
            result["token_received"] = bool(token)
            print(f"✅ Login successful. Token received: {token[:20]}...")
            return token
        else:
            print(f"❌ Login failed with status {response.status_code}")
            return None

    def test_access_with_valid_token(self, token):
        """Test 2: Access protected resource with valid token"""
        print("\n[TEST 2] Accessing protected resource with valid token...")
        
        headers = {"Authorization": f"Bearer {token}"}
        response = requests.get(f"{API_BASE}/api/cart", headers=headers)
        
        result = {
            "test": "access_with_valid_token",
            "status": response.status_code,
            "success": response.status_code == 200
        }
        
        self.results["tests"].append(result)
        
        if response.status_code == 200:
            print("✅ Access granted with valid token")
            return True
        else:
            print(f"❌ Access denied with status {response.status_code}")
            return False

    def test_replay_from_different_device(self, token):
        """Test 3: Replay token from different User-Agent (simulating different device)"""
        print("\n[TEST 3] Attempting token replay from different device...")
        
        # Simulate different device by changing User-Agent
        headers = {
            "Authorization": f"Bearer {token}",
            "User-Agent": "AttackerBot/1.0 (Linux; Android 10; Malicious Device)",
            "X-Forwarded-For": "192.168.100.100"  # Different IP
        }
        
        response = requests.get(f"{API_BASE}/api/cart", headers=headers)
        
        result = {
            "test": "replay_from_different_device",
            "status": response.status_code,
            "success": response.status_code in [401, 403]  # Should be rejected
        }
        
        self.results["tests"].append(result)
        
        if response.status_code in [401, 403]:
            print("✅ Token replay blocked (Expected behavior)")
            return True
        else:
            print(f"⚠️  Token replay not blocked! Status: {response.status_code}")
            print("   This is a security vulnerability - device binding may not be implemented")
            return False

    def test_expired_token_replay(self):
        """Test 4: Replay with expired token"""
        print("\n[TEST 4] Testing expired token...")
        
        # This is a mock expired token (in real scenario, wait for token to expire)
        expired_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImV4cCI6MTYwMDAwMDAwMH0.expired"
        
        headers = {"Authorization": f"Bearer {expired_token}"}
        response = requests.get(f"{API_BASE}/api/cart", headers=headers)
        
        result = {
            "test": "expired_token_replay",
            "status": response.status_code,
            "success": response.status_code == 401
        }
        
        self.results["tests"].append(result)
        
        if response.status_code == 401:
            print("✅ Expired token rejected (Expected behavior)")
            return True
        else:
            print(f"❌ Expired token not properly rejected! Status: {response.status_code}")
            return False

    def test_timestamp_replay_protection(self, token):
        """Test 5: HMAC signature replay protection (for service-to-service calls)"""
        print("\n[TEST 5] Testing HMAC timestamp replay protection...")
        
        # Simulate old timestamp (replayed request)
        old_timestamp = str(int(time.time() * 1000) - 600000)  # 10 minutes ago
        
        headers = {
            "Authorization": f"Bearer {token}",
            "X-Timestamp": old_timestamp,
            "X-Signature": "replayed_signature_value"
        }
        
        # Try to access an internal API that uses HMAC verification
        response = requests.post(
            f"{API_BASE}/api/payments",
            headers=headers,
            json={"orderId": 1, "paymentToken": "test"}
        )
        
        result = {
            "test": "timestamp_replay_protection",
            "status": response.status_code,
            "success": response.status_code in [401, 403]
        }
        
        self.results["tests"].append(result)
        
        if response.status_code in [401, 403]:
            print("✅ Old timestamp rejected (Replay protection working)")
            return True
        else:
            print(f"⚠️  Old timestamp accepted! Status: {response.status_code}")
            return False

    def generate_report(self):
        """Generate test report"""
        print("\n" + "="*60)
        print("TOKEN REPLAY ATTACK TEST RESULTS")
        print("="*60)
        
        total_tests = len(self.results["tests"])
        passed_tests = sum(1 for t in self.results["tests"] if t["success"])
        
        print(f"\nTotal Tests: {total_tests}")
        print(f"Passed: {passed_tests}")
        print(f"Failed: {total_tests - passed_tests}")
        print(f"Success Rate: {(passed_tests/total_tests*100):.1f}%")
        
        print("\nDetailed Results:")
        for test in self.results["tests"]:
            status = "✅ PASS" if test["success"] else "❌ FAIL"
            print(f"  {status} - {test['test']} (HTTP {test['status']})")
        
        # Save to file
        with open("token_replay_results.json", "w") as f:
            json.dump(self.results, f, indent=2)
        
        print(f"\nResults saved to: token_replay_results.json")
        
        return passed_tests == total_tests

    def run_all_tests(self):
        """Run all token replay tests"""
        print("Starting Token Replay Attack Tests...")
        print("="*60)
        
        # Test 1: Login and get token
        token = self.test_valid_login()
        if not token:
            print("\n❌ Cannot proceed without valid token")
            return False
        
        # Test 2: Normal access
        self.test_access_with_valid_token(token)
        
        # Test 3: Replay from different device
        self.test_replay_from_different_device(token)
        
        # Test 4: Expired token
        self.test_expired_token_replay()
        
        # Test 5: Timestamp replay protection
        self.test_timestamp_replay_protection(token)
        
        # Generate report
        return self.generate_report()


if __name__ == "__main__":
    tester = TokenReplayTest()
    success = tester.run_all_tests()
    
    exit(0 if success else 1)
