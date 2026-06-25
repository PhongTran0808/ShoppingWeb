"""
API Rate Limiting & Abuse Prevention Test
Tests whether the system can defend against automated API abuse.
"""

import requests
import time
import json
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

API_BASE = "http://localhost:8080"

class RateLimitingTest:
    def __init__(self):
        self.results = {
            "timestamp": datetime.now().isoformat(),
            "tests": []
        }

    def test_login_rate_limiting(self):
        """Test 1: Credential stuffing attack simulation"""
        print("\n[TEST 1] Testing login rate limiting (Credential Stuffing)...")
        
        # Simulate rapid login attempts
        attempts = 30
        blocked = 0
        allowed = 0
        
        for i in range(attempts):
            try:
                response = requests.post(
                    f"{API_BASE}/api/catalog/users/login",
                    json={"username": f"attacker{i}", "password": "wrong"},
                    timeout=2
                )
                
                if response.status_code == 429:  # Too Many Requests
                    blocked += 1
                elif response.status_code == 401:  # Unauthorized (allowed but failed)
                    allowed += 1
            except Exception as e:
                print(f"Request {i} error: {e}")
        
        result = {
            "test": "login_rate_limiting",
            "total_attempts": attempts,
            "blocked": blocked,
            "allowed": allowed,
            "success": blocked > attempts * 0.5  # At least 50% should be blocked
        }
        
        self.results["tests"].append(result)
        
        print(f"Total Attempts: {attempts}")
        print(f"Blocked (429): {blocked}")
        print(f"Allowed: {allowed}")
        
        if result["success"]:
            print("✅ Rate limiting working effectively")
        else:
            print("⚠️  Rate limiting may not be configured properly")
        
        return result["success"]

    def test_catalog_query_abuse(self):
        """Test 2: Excessive catalog queries"""
        print("\n[TEST 2] Testing catalog API rate limiting...")
        
        def make_request(i):
            try:
                response = requests.get(f"{API_BASE}/api/catalog/products", timeout=2)
                return response.status_code
            except Exception:
                return 0
        
        # Concurrent requests
        with ThreadPoolExecutor(max_workers=20) as executor:
            status_codes = list(executor.map(make_request, range(100)))
        
        blocked = status_codes.count(429)
        allowed = status_codes.count(200)
        
        result = {
            "test": "catalog_query_abuse",
            "total_requests": 100,
            "blocked": blocked,
            "allowed": allowed,
            "success": blocked > 20  # Some should be blocked
        }
        
        self.results["tests"].append(result)
        
        print(f"Concurrent Requests: 100")
        print(f"Blocked: {blocked}")
        print(f"Allowed: {allowed}")
        
        if result["success"]:
            print("✅ Catalog API rate limiting working")
        else:
            print("⚠️  Consider lowering rate limits")
        
        return result["success"]

    def test_cart_manipulation_spam(self):
        """Test 3: Cart manipulation spam"""
        print("\n[TEST 3] Testing cart manipulation rate limiting...")
        
        # Login first to get token
        login_response = requests.post(
            f"{API_BASE}/api/catalog/users/login",
            json={
                "username": "user1",
                "password": "8ff8ef3d62115854978812349a72122df63c89ce2fbad21405de7f38f66b2c9027800a09fd7d9ecd5b8596ae24a963915a978366d86a7836e02ed0445543a3f8"
            }
        )
        
        if login_response.status_code != 200:
            print("❌ Cannot login to test cart manipulation")
            return False
        
        token = login_response.json().get("token")
        headers = {"Authorization": f"Bearer {token}"}
        
        # Rapid cart updates
        attempts = 50
        blocked = 0
        allowed = 0
        
        for i in range(attempts):
            try:
                response = requests.post(
                    f"{API_BASE}/api/cart/items",
                    headers=headers,
                    json={"productId": 1, "quantity": 1},
                    timeout=2
                )
                
                if response.status_code == 429:
                    blocked += 1
                else:
                    allowed += 1
            except Exception:
                pass
            
            time.sleep(0.05)  # Small delay
        
        result = {
            "test": "cart_manipulation_spam",
            "total_attempts": attempts,
            "blocked": blocked,
            "allowed": allowed,
            "success": blocked > 0  # Some should be blocked
        }
        
        self.results["tests"].append(result)
        
        print(f"Rapid Updates: {attempts}")
        print(f"Blocked: {blocked}")
        print(f"Allowed: {allowed}")
        
        if result["success"]:
            print("✅ Cart rate limiting effective")
        else:
            print("⚠️  Consider adding cart-specific rate limits")
        
        return result["success"]

    def test_progressive_delay(self):
        """Test 4: Progressive delay on repeated failures"""
        print("\n[TEST 4] Testing progressive delay/backoff...")
        
        delays = []
        
        for i in range(5):
            start = time.time()
            response = requests.post(
                f"{API_BASE}/api/catalog/users/login",
                json={"username": "attacker", "password": "wrong"}
            )
            end = time.time()
            
            delay = end - start
            delays.append(delay)
            print(f"Attempt {i+1}: {delay:.3f}s (Status: {response.status_code})")
            
            time.sleep(0.5)
        
        # Check if delays are increasing
        increasing = all(delays[i] <= delays[i+1] for i in range(len(delays)-1))
        
        result = {
            "test": "progressive_delay",
            "delays": delays,
            "success": increasing or delays[-1] > delays[0] * 1.5
        }
        
        self.results["tests"].append(result)
        
        if result["success"]:
            print("✅ Progressive delay working")
        else:
            print("ℹ️  Progressive delay not detected (may not be implemented)")
        
        return result["success"]

    def generate_report(self):
        """Generate test report"""
        print("\n" + "="*60)
        print("API RATE LIMITING TEST RESULTS")
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
            print(f"  {status} - {test['test']}")
        
        # Save to file
        with open("rate_limiting_results.json", "w") as f:
            json.dump(self.results, f, indent=2)
        
        print(f"\nResults saved to: rate_limiting_results.json")
        
        return passed_tests >= total_tests * 0.5  # At least 50% pass

    def run_all_tests(self):
        """Run all rate limiting tests"""
        print("Starting API Rate Limiting Tests...")
        print("="*60)
        
        self.test_login_rate_limiting()
        time.sleep(2)
        
        self.test_catalog_query_abuse()
        time.sleep(2)
        
        self.test_cart_manipulation_spam()
        time.sleep(2)
        
        self.test_progressive_delay()
        
        return self.generate_report()


if __name__ == "__main__":
    tester = RateLimitingTest()
    success = tester.run_all_tests()
    
    exit(0 if success else 1)
