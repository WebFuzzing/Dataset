#!/usr/bin/env python3
"""
AdoptMe Advanced Algorithm Test Suite
Performs deep validation of algorithm correctness
"""

import requests
import json
import sys
from typing import Dict, List, Any, Tuple
from datetime import datetime

API_BASE = "http://localhost:8080"

class Colors:
    GREEN = '\033[0;32m'
    RED = '\033[0;31m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'

class TestResults:
    def __init__(self):
        self.total = 0
        self.passed = 0
        self.failed = 0
        self.results = []

    def add_result(self, test_name: str, passed: bool, message: str = ""):
        self.total += 1
        if passed:
            self.passed += 1
            status = f"{Colors.GREEN}✓ PASSED{Colors.NC}"
        else:
            self.failed += 1
            status = f"{Colors.RED}✗ FAILED{Colors.NC}"

        self.results.append({
            'name': test_name,
            'passed': passed,
            'message': message
        })

        print(f"[TEST {self.total}] {test_name}: {status}")
        if message:
            print(f"  → {message}")

    def print_summary(self):
        print(f"\n{Colors.YELLOW}{'='*50}{Colors.NC}")
        print(f"{Colors.YELLOW}Test Summary{Colors.NC}")
        print(f"{Colors.YELLOW}{'='*50}{Colors.NC}")
        print(f"Total Tests:  {Colors.BLUE}{self.total}{Colors.NC}")
        print(f"Passed:       {Colors.GREEN}{self.passed}{Colors.NC}")
        print(f"Failed:       {Colors.RED}{self.failed}{Colors.NC}")

        if self.total > 0:
            pass_rate = (self.passed / self.total) * 100
            print(f"Pass Rate:    {Colors.BLUE}{pass_rate:.1f}%{Colors.NC}")

        return self.failed == 0

def check_server() -> bool:
    """Check if server is running"""
    try:
        response = requests.get(f"{API_BASE}/ping", timeout=5)
        return response.status_code == 200
    except:
        return False

def api_get(endpoint: str) -> Tuple[int, Any]:
    """Make API GET request and return status code and JSON"""
    try:
        response = requests.get(f"{API_BASE}{endpoint}", timeout=30)
        return response.status_code, response.json() if response.status_code == 200 else None
    except requests.exceptions.Timeout:
        return 408, None
    except Exception as e:
        return 500, None

# ==================== VALIDATION FUNCTIONS ====================

def validate_path_exists(data: Dict) -> bool:
    """Validate BFS/DFS response"""
    if not isinstance(data, dict):
        return False
    if 'exists' not in data:
        return False
    if data['exists'] and 'path' not in data:
        return False
    if data['exists'] and not isinstance(data.get('path'), list):
        return False
    return True

def validate_dijkstra(data: Dict) -> bool:
    """Validate Dijkstra response has path and positive weight"""
    if not isinstance(data, dict):
        return False
    if 'path' not in data or 'totalWeight' not in data:
        return False
    if not isinstance(data['path'], list) or len(data['path']) < 2:
        return False
    if data['totalWeight'] <= 0:
        return False
    return True

def validate_tsp(data: Dict) -> bool:
    """Validate TSP response"""
    if not isinstance(data, dict):
        return False
    if 'route' not in data or 'totalDistanceKm' not in data:
        return False
    route = data['route']
    if not isinstance(route, list) or len(route) < 3:
        return False
    # TSP should return to start
    if route[0] != route[-1]:
        return False
    if data['totalDistanceKm'] <= 0:
        return False
    return True

def validate_mst(data: Dict, expected_nodes: int = 15) -> bool:
    """Validate MST response"""
    if not isinstance(data, dict):
        return False
    if 'edges' not in data or 'totalWeight' not in data:
        return False
    edges = data['edges']
    if not isinstance(edges, list):
        return False
    # MST should have n-1 edges for n nodes
    if len(edges) != expected_nodes - 1:
        return False
    if data['totalWeight'] <= 0:
        return False
    return True

def validate_sorted_list(data: Dict, criteria: str) -> bool:
    """Validate sorting response is actually sorted"""
    if not isinstance(data, dict):
        return False
    if 'dogs' not in data:
        return False
    dogs = data['dogs']
    if not isinstance(dogs, list) or len(dogs) == 0:
        return False

    # Check if sorted in ascending order
    for i in range(len(dogs) - 1):
        current = dogs[i].get(criteria)
        next_val = dogs[i + 1].get(criteria)
        if current is None or next_val is None:
            return False
        if current > next_val:
            return False

    return True

def validate_greedy(data: Dict) -> bool:
    """Validate Greedy matching response"""
    if not isinstance(data, dict):
        return False
    required_fields = ['assignedDogs', 'totalScore', 'totalCost']
    for field in required_fields:
        if field not in data:
            return False
    if not isinstance(data['assignedDogs'], list):
        return False
    if data['totalScore'] < 0 or data['totalCost'] < 0:
        return False
    return True

def validate_backtracking(data: Dict) -> bool:
    """Validate Backtracking response"""
    if not isinstance(data, dict):
        return False
    if 'assignments' not in data or 'totalScore' not in data:
        return False
    if not isinstance(data['assignments'], dict):
        return False
    if data['totalScore'] < 0:
        return False
    return True

def validate_knapsack(data: Dict, capacity: int) -> bool:
    """Validate Knapsack response"""
    if not isinstance(data, dict):
        return False
    required = ['selectedDogs', 'totalPriority', 'totalWeightKg', 'vehicleCapacityKg']
    for field in required:
        if field not in data:
            return False
    if not isinstance(data['selectedDogs'], list):
        return False
    # Total weight should not exceed capacity
    if data['totalWeightKg'] > capacity + 0.1:  # Allow small floating point error
        return False
    if data['totalPriority'] < 0:
        return False
    return True

# ==================== TEST SUITES ====================

def test_graph_algorithms(results: TestResults):
    """Test BFS and DFS"""
    print(f"\n{Colors.YELLOW}=== Graph Traversal Tests ==={Colors.NC}")

    tests = [
        ("BFS: A→B", "/graph/reachable?from=A&to=B&method=bfs"),
        ("BFS: A→O", "/graph/reachable?from=A&to=O&method=bfs"),
        ("BFS: E→M", "/graph/reachable?from=E&to=M&method=bfs"),
        ("DFS: A→B", "/graph/reachable?from=A&to=B&method=dfs"),
        ("DFS: I→K", "/graph/reachable?from=I&to=K&method=dfs"),
    ]

    for name, endpoint in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_path_exists(data):
            path_len = len(data.get('path', []))
            results.add_result(name, True, f"Path length: {path_len}")
        else:
            results.add_result(name, False, f"HTTP {status} or invalid response")

def test_dijkstra(results: TestResults):
    """Test Dijkstra shortest path"""
    print(f"\n{Colors.YELLOW}=== Dijkstra Shortest Path Tests ==={Colors.NC}")

    tests = [
        ("Dijkstra: H→A", "/routes/shortest?from=H&to=A"),
        ("Dijkstra: A→M", "/routes/shortest?from=A&to=M"),
        ("Dijkstra: E→O", "/routes/shortest?from=E&to=O"),
    ]

    for name, endpoint in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_dijkstra(data):
            distance = data['totalWeight']
            path = ' → '.join(data['path'])
            results.add_result(name, True, f"{distance}km via {path}")
        else:
            results.add_result(name, False, f"HTTP {status} or invalid response")

def test_tsp(results: TestResults):
    """Test TSP Branch & Bound"""
    print(f"\n{Colors.YELLOW}=== TSP Branch & Bound Tests ==={Colors.NC}")

    tests = [
        ("TSP: 4 nodes", "/routes/tsp/bnb?nodes=A,B,C,H"),
        ("TSP: 5 nodes", "/routes/tsp/bnb?nodes=A,D,E,I,H"),
        ("TSP: 7 nodes", "/routes/tsp/bnb?nodes=A,B,C,G,L,M,H"),
    ]

    for name, endpoint in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_tsp(data):
            distance = data['totalDistanceKm']
            route = ' → '.join(data['route'])
            results.add_result(name, True, f"{distance}km: {route}")
        else:
            results.add_result(name, False, f"HTTP {status} or invalid response")

def test_mst(results: TestResults):
    """Test MST algorithms"""
    print(f"\n{Colors.YELLOW}=== MST Tests ==={Colors.NC}")

    tests = [
        ("MST: Kruskal", "/network/mst?algorithm=kruskal"),
        ("MST: Prim", "/network/mst?algorithm=prim"),
    ]

    weights = []
    for name, endpoint in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_mst(data):
            weight = data['totalWeight']
            weights.append(weight)
            edge_count = len(data['edges'])
            results.add_result(name, True, f"{weight}km total, {edge_count} edges")
        else:
            results.add_result(name, False, f"HTTP {status} or invalid response")

    # Both MST algorithms should produce same total weight
    if len(weights) == 2 and abs(weights[0] - weights[1]) < 0.01:
        results.add_result("MST: Kruskal == Prim weight", True, f"Both: {weights[0]}km")
    elif len(weights) == 2:
        results.add_result("MST: Kruskal == Prim weight", False,
                          f"Kruskal: {weights[0]}km, Prim: {weights[1]}km")

def test_sorting(results: TestResults):
    """Test sorting algorithms"""
    print(f"\n{Colors.YELLOW}=== Sorting Algorithm Tests ==={Colors.NC}")

    tests = [
        ("Sort: Priority (MergeSort)", "/dogs/sort?criteria=priority&algorithm=mergesort", "priority"),
        ("Sort: Priority (QuickSort)", "/dogs/sort?criteria=priority&algorithm=quicksort", "priority"),
        ("Sort: Age (MergeSort)", "/dogs/sort?criteria=age&algorithm=mergesort", "age"),
        ("Sort: Weight (QuickSort)", "/dogs/sort?criteria=weight&algorithm=quicksort", "weight"),
    ]

    for name, endpoint, criteria in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_sorted_list(data, criteria):
            count = len(data['dogs'])
            first = data['dogs'][0].get(criteria)
            last = data['dogs'][-1].get(criteria)
            results.add_result(name, True, f"{count} dogs, range: {first}-{last}")
        else:
            results.add_result(name, False, f"HTTP {status} or not properly sorted")

def test_greedy(results: TestResults):
    """Test Greedy matching"""
    print(f"\n{Colors.YELLOW}=== Greedy Matching Tests ==={Colors.NC}")

    tests = [
        ("Greedy: High budget (P9)", "/adoptions/greedy?adopterId=P9"),
        ("Greedy: Low budget (P10)", "/adoptions/greedy?adopterId=P10"),
        ("Greedy: With kids (P1)", "/adoptions/greedy?adopterId=P1"),
    ]

    for name, endpoint in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_greedy(data):
            dogs = len(data['assignedDogs'])
            score = data['totalScore']
            cost = data['totalCost']
            results.add_result(name, True, f"{dogs} dogs, score: {score:.1f}, cost: ${cost:.0f}")
        else:
            results.add_result(name, False, f"HTTP {status} or invalid response")

def test_backtracking(results: TestResults):
    """Test Backtracking"""
    print(f"\n{Colors.YELLOW}=== Backtracking Tests ==={Colors.NC}")

    status, data = api_get("/adoptions/constraints/backtracking")
    if status == 200 and validate_backtracking(data):
        assignments = len(data['assignments'])
        score = data['totalScore']
        results.add_result("Backtracking: Multi-adopter", True,
                          f"{assignments} adopters matched, score: {score:.1f}")
    else:
        results.add_result("Backtracking: Multi-adopter", False,
                          f"HTTP {status} or invalid response")

def test_knapsack(results: TestResults):
    """Test Knapsack DP"""
    print(f"\n{Colors.YELLOW}=== Knapsack DP Tests ==={Colors.NC}")

    tests = [
        ("Knapsack: 30kg", "/transport/optimal-dp?capacityKg=30", 30),
        ("Knapsack: 60kg", "/transport/optimal-dp?capacityKg=60", 60),
        ("Knapsack: 100kg", "/transport/optimal-dp?capacityKg=100", 100),
        ("Knapsack: 500kg", "/transport/optimal-dp?capacityKg=500", 500),
    ]

    for name, endpoint, capacity in tests:
        status, data = api_get(endpoint)
        if status == 200 and validate_knapsack(data, capacity):
            dogs = len(data['selectedDogs'])
            priority = data['totalPriority']
            weight = data['totalWeightKg']
            results.add_result(name, True,
                              f"{dogs} dogs, priority: {priority}, weight: {weight}kg")
        else:
            results.add_result(name, False, f"HTTP {status} or invalid response")

def test_data_integrity(results: TestResults):
    """Test data integrity"""
    print(f"\n{Colors.YELLOW}=== Data Integrity Tests ==={Colors.NC}")

    # Check shelters
    status, data = api_get("/shelters")
    if status == 200 and isinstance(data, list):
        count = len(data)
        results.add_result("Data: Shelters count", count == 15, f"Found {count}/15 shelters")
    else:
        results.add_result("Data: Shelters count", False, f"HTTP {status}")

    # Check dogs
    status, data = api_get("/dogs")
    if status == 200 and isinstance(data, list):
        count = len(data)
        results.add_result("Data: Dogs count", count == 40, f"Found {count}/40 dogs")
    else:
        results.add_result("Data: Dogs count", False, f"HTTP {status}")

    # Check adopters
    status, data = api_get("/adopters")
    if status == 200 and isinstance(data, list):
        count = len(data)
        results.add_result("Data: Adopters count", count == 15, f"Found {count}/15 adopters")
    else:
        results.add_result("Data: Adopters count", False, f"HTTP {status}")

# ==================== MAIN ====================

def main():
    print(f"{Colors.YELLOW}{'='*50}{Colors.NC}")
    print(f"{Colors.YELLOW}AdoptMe Advanced Algorithm Test Suite{Colors.NC}")
    print(f"{Colors.YELLOW}{'='*50}{Colors.NC}")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()

    # Check server
    print(f"{Colors.BLUE}Checking server connection...{Colors.NC}")
    if not check_server():
        print(f"{Colors.RED}✗ Server is not running at {API_BASE}{Colors.NC}")
        print("Please start the backend server first.")
        sys.exit(1)
    print(f"{Colors.GREEN}✓ Server is running{Colors.NC}")

    # Run all tests
    results = TestResults()

    test_data_integrity(results)
    test_graph_algorithms(results)
    test_dijkstra(results)
    test_tsp(results)
    test_mst(results)
    test_sorting(results)
    test_greedy(results)
    test_backtracking(results)
    test_knapsack(results)

    # Print summary
    all_passed = results.print_summary()

    if all_passed:
        print(f"\n{Colors.GREEN}✓ All tests passed!{Colors.NC}")
        sys.exit(0)
    else:
        print(f"\n{Colors.RED}✗ Some tests failed.{Colors.NC}")
        sys.exit(1)

if __name__ == "__main__":
    main()
