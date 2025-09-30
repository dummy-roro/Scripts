# Chaos Testing

### **Prerequisites**

1. SSH access to the server.
2. `stress-ng` and `tc` installed:
    
    ```bash
    sudo yum install -y stress-ng iproute  # for RHEL/CentOS
    sudo apt install -y stress-ng iproute2 # for Ubuntu/Debian
    ```
    
3. Your Nginx logs (`access.log` and `error.log`) should be monitored:
    
    ```bash
    tail -f /var/log/nginx/access.log /var/log/nginx/error.log
    ```
    
4. Optional: install `hey` or `wrk` for load generation:
    
    ```bash
    sudo yum install -y hey
    ```
    

---

### **Step 1: Baseline Load**

- Determine normal behavior before chaos.

```bash
hey -n 1000 -c 10 http://localhost/
```

- Observe:
    - Response times
    - Error rates
    - CPU & memory usage (`top`, `htop`, `free -m`)

---

### **Step 2: CPU Stress**

- Slowly stress CPU to see performance limits:

```bash
sudo stress-ng --cpu 1 --timeout 60s --metrics-brief
```

- Increase to both CPUs if stable:

```bash
sudo stress-ng --cpu 2 --timeout 60s --metrics-brief
```

- Monitor:
    - Node.js process responsiveness
    - Nginx serving requests
    - Any increase in latency or 502/503 errors

**Expected outcome:** App should slow but not crash.

---

### **Step 3: Memory Stress**

- Simulate memory exhaustion safely (start small):

```bash
sudo stress-ng --vm 1 --vm-bytes 512M --timeout 60s --metrics-brief
```

- Gradually increase:

```bash
sudo stress-ng --vm 1 --vm-bytes 1024M --timeout 60s --metrics-brief
```

- Monitor:
    - Swap usage
    - Out-of-memory (OOM) killer logs (`dmesg`)
    - Response errors

**Goal:** Ensure the app handles high memory usage gracefully.

---

### **Step 4: Disk / I/O Stress**

- Stress I/O to test logging and caching:

```bash
sudo stress-ng --hdd 1 --hdd-bytes 500M --timeout 60s
```

- Watch for slow responses from Nginx or Node.js.

---

### **Step 5: Network Chaos**

- Introduce latency to mimic real-world slow connections:

```bash
sudo tc qdisc add dev eth0 root netem delay 200ms
```

- Add packet loss:

```bash
sudo tc qdisc change dev eth0 root netem loss 5%
```

- Test load during network chaos:

```bash
hey -n 500 -c 20 http://localhost/
```

- Remove network chaos after test:

```bash
sudo tc qdisc del dev eth0 root
```

---

### **Step 6: Process Kill / Restart**

- Simulate a Node.js crash:

```bash
pkill -f node
```

- Observe Nginx response codes (502/503).
- Restart Node.js and measure recovery.

---

### **Step 7: Automated Chaos Scenarios (Optional)**

- Use scripts to run CPU, memory, and network chaos together.
- Measure:
    - % of failed requests
    - Recovery time
    - Resource saturation

---

### **Tips / Safety**

1. Start **small and slow**, then escalate.
2. Always monitor metrics during chaos.
3. Keep **SSH session open** in case rollback is needed.
4. Avoid full memory or CPU exhaustion that could lock the server.

## **1. Prepare the test environment**

- Use a separate machine (another server, VM, or your laptop).
- Install a load testing tool:

### **Recommended tools**

- **hey** (simple, good for HTTP GET/POST)
    
    ```bash
    go install github.com/rakyll/hey@latest
    ```
    
- **wrk** (high performance, more advanced)
    
    ```bash
    sudo apt install wrk   # Ubuntu/Debian
    sudo yum install wrk   # RHEL/CentOS
    ```
    
- Ensure the test machine can reach your server (via public IP or VPN/private network).

---

## **2. Start monitoring your server**

- Make sure **Prometheus + Node Exporter + Nginx Exporter** are running.
- Check CPU, RAM, network, and Nginx metrics.

---

## **3. Start with a baseline test**

- Start small to avoid overloading your server:

```bash
hey -n 1000 -c 10 https://your-server-domain/
```

- `n 1000` → total requests
- `c 10` → concurrency (simultaneous requests)

**Check:**

- CPU < 80%
- RAM < 80%
- Nginx response time < 1s

---

## **4. Gradually increase load**

- Step 1: 50 concurrent users

```bash
hey -n 5000 -c 50 https://your-server-domain/
```

- Step 2: 100 concurrent users

```bash
hey -n 10000 -c 100 https://your-server-domain/
```

- Step 3: 150-200 concurrent users (watch server metrics closely)

**Stop immediately if:**

- CPU spikes near 100%
- Memory usage hits >90%
- Requests start failing

---

## **5. Chaos testing ideas**

- Randomly terminate one backend process (simulate crash)
- Block network temporarily (simulate network issues)
- Introduce high CPU or memory load alongside requests

**Important:** Always test during **non-production hours** or in a staging environment if possible.

---

## **6. Record and analyze metrics**

- Use Prometheus + Grafana dashboards to track:
    - CPU/memory utilization
    - Active Nginx connections
    - HTTP request latencies and error rates
- Compare performance under different load levels.

---

## **7. Key tips for a 2vCPU / 2GB RAM server**

- Keep concurrency < 100 for long tests to avoid swapping.
- Use small request payloads initially; heavy requests may max out CPU quickly.
- Monitor Nginx’s `worker_connections` and `worker_processes` to ensure it’s not hitting limits.
