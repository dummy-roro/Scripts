# Swap

how to safely set up a **4 GB swap file** on Linux (no repartition needed):

---

### Steps

1. **Create a 4 GB empty file** (acts as swap):
    
    ```bash
    sudo fallocate -l 4G /swapfile
    ```
    
    *(If `fallocate` isn’t available, use: `sudo dd if=/dev/zero of=/swapfile bs=1M count=4096`)*
    
2. **Set correct permissions** (swap file must not be world-readable):
    
    ```bash
    sudo chmod 600 /swapfile
    ```
    
3. **Format it as swap**:
    
    ```bash
    sudo mkswap /swapfile
    ```
    
4. **Enable the swap file** immediately:
    
    ```bash
    sudo swapon /swapfile
    ```
    
5. **Make it permanent** (so it works after reboot):
    - Open `/etc/fstab` in a text editor:
        
        ```bash
        sudo vi /etc/fstab
        ```
        
    - Add this line at the end:
        
        ```
        /swapfile none swap sw 0 0
        ```
        
6. **Verify it’s working**:
    
    ```bash
    free -h
    ```
    
    You should see ~4 GB swap listed.
