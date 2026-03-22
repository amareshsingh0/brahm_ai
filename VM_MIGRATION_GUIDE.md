# Brahm AI — GCP VM Migration Guide

**Date of Migration**: 2026-03-22
**Author**: Based on real migration experience — old VM (brahm-489317) → new VM (brahm2005)

---

## Overview

This guide documents the complete process of migrating Brahm AI from one GCP VM to another, including every problem encountered and its solution. Use this as a reference for any future migrations.

---

## Source and Destination

| Property | Old VM | New VM |
|----------|--------|--------|
| Machine Type | g2-standard-32 | n2-highmem-8 |
| vCPU | 32 | 8 |
| RAM | 128GB | 64GB |
| GPU | NVIDIA L4 24GB | None |
| Disk | — | 250GB pd-ssd |
| Zone | us-central1-a | us-central1-a |
| GCP Project | brahm-489317 | brahm2005 |
| GCP Account | amareshsingh2005@gmail.com | photogeniusai@gmail.com |
| OS User | amareshsingh2005 | photogeniusai (sudo) + amareshsingh2005 |
| External IP | — | 34.134.231.111 |
| Domain | brahmasmi.bimoraai.com | brahmasmi.bimoraai.com |

---

## Data Transferred

| Directory | Size | Notes |
|-----------|------|-------|
| `books/indexes/` | 16GB | FAISS indexes, documents.jsonl (5.1GB), bm25_cache.pkl (5.4GB), metadata.db (561MB) |
| `books/data/raw/` | 36GB | PDF source files |
| `books/data/nasa_ephe/` | 6.2GB | NASA ephemeris data |
| `books/data/aws_datasets/` | 5.1GB | AWS dataset cache |
| `books/data/processed/` | 438MB | Processed text chunks |
| `books/data/chunks/` | 33MB | Chunked data |
| `books/data/embeddings/` | 27MB | Precomputed embeddings |
| `books/data/palmistry/` | 1.5GB | Kaggle 11k Hands dataset |
| `books/data/dictionaries/` | 697MB | Sanskrit/Hindi dictionaries |
| `books/data/github/` | 1.3GB | GitHub dataset |
| `books/data/hf_datasets/` | 8.4MB | Hugging Face dataset cache |
| `books/data/swiss_ephe/` | 4.2MB | Swiss Ephemeris files |
| Code | — | `git clone https://github.com/amareshsingh0/brahm_ai` |

---

## Step-by-Step Migration Process

### Step 1 — Create the New VM

Run from **Cloud Shell** (not from inside any VM):

```bash
gcloud compute instances create brahm \
  --project=brahm2005 \
  --zone=us-central1-a \
  --machine-type=n2-highmem-8 \
  --image-family=debian-12 \
  --image-project=debian-cloud \
  --boot-disk-size=250GB \
  --boot-disk-type=pd-ssd \
  --metadata=ssh-keys="amareshsingh2005:YOUR_SSH_PUBLIC_KEY" \
  --tags=http-server,https-server
```

**Note on machine type**: GPU VMs (g2/a2 series) are frequently exhausted. For Brahm AI, GPU is NOT required — Gemini API handles LLM inference, and sentence-transformers run fine on CPU. The real bottleneck is RAM (BM25 index requires ~12GB at peak). `n2-highmem-8` with 64GB RAM is the sweet spot.

### Step 2 — Add Firewall Rules

Run from **Cloud Shell**:

```bash
gcloud compute firewall-rules create allow-http-https \
  --project=brahm2005 \
  --allow=tcp:80,tcp:443 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=http-server \
  --description="Allow HTTP and HTTPS traffic"

# Only if you need direct API access (not recommended for production)
gcloud compute firewall-rules create allow-api-port \
  --project=brahm2005 \
  --allow=tcp:8000 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=http-server \
  --description="Allow FastAPI port"
```

### Step 3 — Install System Packages on New VM

```bash
sudo apt-get update && sudo apt-get install -y \
  python3 python3-pip python3-venv \
  git nginx certbot python3-certbot-nginx \
  build-essential libssl-dev libffi-dev python3-dev \
  curl wget screen rsync htop
```

### Step 4 — Set Up Python Virtual Environment

```bash
python3 -m venv ~/ai-env
source ~/ai-env/bin/activate

pip install \
  fastapi uvicorn \
  google-genai \
  pyswisseph \
  faiss-cpu \
  sentence-transformers \
  rank-bm25 \
  numpy scipy \
  python-multipart \
  "python-jose[cryptography]" \
  "passlib[bcrypt]" \
  aiofiles pillow \
  firebase-admin \
  sqlalchemy aiosqlite
```

### Step 5 — Clone Code Repository

```bash
cd ~
git clone https://github.com/amareshsingh0/brahm_ai books
```

This clones into `~/books/` matching the existing directory structure.

### Step 6 — Set Up SSH Key Auth Between VMs

This allows rsync to transfer data directly between VMs at 200-400 MB/s (same GCP region).

**On the old VM** (source):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -N ""
cat ~/.ssh/id_ed25519.pub
# Copy the entire output line
```

**On the new VM** (destination), as a sudo user:

```bash
sudo mkdir -p /home/amareshsingh2005/.ssh
echo "PASTE_PUBLIC_KEY_HERE" | sudo tee -a /home/amareshsingh2005/.ssh/authorized_keys
sudo chown -R amareshsingh2005:amareshsingh2005 /home/amareshsingh2005/.ssh
sudo chmod 700 /home/amareshsingh2005/.ssh
sudo chmod 600 /home/amareshsingh2005/.ssh/authorized_keys
```

**Test the connection** from old VM:

```bash
ssh -i ~/.ssh/id_ed25519 amareshsingh2005@34.134.231.111 "echo connection works"
```

### Step 7 — Transfer Data

**CRITICAL**: Before starting any large transfer, **disable the brahm-api service** on the new VM to keep SSH responsive:

```bash
# On new VM
sudo systemctl stop brahm-api && sudo systemctl disable brahm-api
```

**For each large directory**, use the pack-then-transfer pattern:

```bash
# On old VM — pack WITHOUT gzip compression (faster on same-region network)
cd ~
tar -cf /tmp/indexes.tar books/indexes/
tar -cf /tmp/data.tar books/data/

# Transfer via rsync
rsync -av --progress /tmp/indexes.tar amareshsingh2005@34.134.231.111:/tmp/
rsync -av --progress /tmp/data.tar amareshsingh2005@34.134.231.111:/tmp/

# On new VM — extract with sudo
sudo tar -xf /tmp/indexes.tar -C /home/amareshsingh2005/
sudo tar -xf /tmp/data.tar -C /home/amareshsingh2005/

# Fix ownership after extraction
sudo chown -R amareshsingh2005:amareshsingh2005 /home/amareshsingh2005/books/

# Clean up tar files to free /tmp space
sudo rm -f /tmp/*.tar
```

**Why no gzip?** GCP same-region network throughput is 300-400 MB/s. Gzip compression adds CPU overhead that outweighs any bandwidth savings — uncompressed tar is faster end-to-end.

Achieved transfer speeds: **216–379 MB/s**

### Step 8 — Configure Environment Variables

Environment variables must be in `/etc/environment` for systemd services to access them — `.bashrc` is NOT read by systemd.

```bash
sudo bash -c 'echo "GEMINI_API_KEY=YOUR_GEMINI_KEY_HERE" >> /etc/environment'
```

Verify:

```bash
grep GEMINI_API_KEY /etc/environment
```

### Step 9 — Set Up systemd Service

Create `/etc/systemd/system/brahm-api.service`:

```ini
[Unit]
Description=Brahm AI API
After=network.target

[Service]
Type=simple
User=amareshsingh2005
WorkingDirectory=/home/amareshsingh2005/books
Environment="PATH=/home/amareshsingh2005/ai-env/bin:/usr/bin:/bin"
EnvironmentFile=/etc/environment
ExecStart=/home/amareshsingh2005/ai-env/bin/uvicorn api.main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**IMPORTANT**: Never use `--reload` flag with uvicorn — it breaks SSE (Server-Sent Events) streaming for AI chat.

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable brahm-api
sudo systemctl start brahm-api
sudo systemctl status brahm-api
```

### Step 10 — Configure Nginx

Create `/etc/nginx/sites-available/brahm-ai`:

```nginx
server {
    listen 80;
    server_name brahmasmi.bimoraai.com;

    # Frontend static files
    root /var/www/brahm-ai;
    index index.html;

    # API proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection '';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
        chunked_transfer_encoding on;
    }

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/brahm-ai /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### Step 11 — SSL Certificate (Certbot)

**Prerequisite**: Port 80 must be open (firewall rule from Step 2) and DNS must point to new VM IP.

**CRITICAL**: Stop brahm-api before running certbot if RAM is tight:

```bash
sudo systemctl stop brahm-api
sudo certbot --nginx -d brahmasmi.bimoraai.com
sudo systemctl start brahm-api
```

### Step 12 — Update DNS

In your DNS provider (Namecheap/Cloudflare/etc.), update the A record:

```
brahmasmi.bimoraai.com  →  34.134.231.111
```

DNS propagation takes 5-30 minutes typically. Verify:

```bash
dig brahmasmi.bimoraai.com +short
curl https://brahmasmi.bimoraai.com/api/health
```

### Step 13 — Deploy Frontend

On your local machine:

```bash
cd "c:\desktop\Brahm AI"
pnpm run build
# Then copy dist/ to new VM
rsync -av dist/ amareshsingh2005@34.134.231.111:/var/www/brahm-ai/
```

Or on the new VM after pulling from git:

```bash
# On new VM
sudo mkdir -p /var/www/brahm-ai
sudo cp -r /home/amareshsingh2005/books/dist/* /var/www/brahm-ai/
sudo chown -R www-data:www-data /var/www/brahm-ai/
```

---

## Problems Faced and Solutions

### Problem 1 — GPU Zone Exhaustion

**Error**: `ZONE_RESOURCE_POOL_EXHAUSTED` for `nvidia-tesla-t4` and `nvidia-l4` in us-central1-a, us-central1-b, us-central1-c, us-east1-c, us-west1-b

**Root cause**: GPU VM quotas are frequently exhausted, especially for L4 and T4 GPUs.

**Solution**: Don't use a GPU at all.

Brahm AI uses Gemini API (cloud inference) — no local GPU needed for LLM. Sentence-transformers (paraphrase-multilingual-MiniLM-L12-v2, cross-encoder reranker) run fine on CPU. The real memory bottleneck is BM25 + FAISS loading ~12GB RAM. Use `n2-highmem-8` (64GB RAM) instead.

**Savings**: GPU VMs cost $0.60+/hr; n2-highmem-8 is significantly cheaper.

---

### Problem 2 — BM25 Loading Crashes SSH / Guest Agent

**Symptom**: After starting brahm-api, SSH becomes completely unavailable for 10-20 minutes. GCP serial console shows Google Guest Agent crashing in a loop.

**Root cause**: Loading `bm25_cache.pkl` (5.4GB) + `documents.jsonl` (5.1GB) into RAM causes memory pressure. The Guest Agent (which handles SSH key injection) runs out of memory and crashes repeatedly.

**Solution**: Always stop and disable brahm-api before doing any SSH-intensive work (data transfer, certbot, nginx config, etc.).

```bash
# Before heavy operations
sudo systemctl stop brahm-api && sudo systemctl disable brahm-api

# After work is done
sudo systemctl enable brahm-api && sudo systemctl start brahm-api
```

**Rule**: If you need SSH to be reliable, brahm-api must be off.

---

### Problem 3 — HTTP Server Serving Wrong Directory

**Symptom**: Started `python3 -m http.server 9090` from home directory (`/home/user/`), but tar files were in `/tmp/`. wget on destination got 404.

**Solution**: Either start the HTTP server from `/` (filesystem root) so all paths are accessible:

```bash
cd / && python3 -m http.server 9090
```

Or better yet, skip HTTP server entirely and use rsync over SSH (faster, more reliable):

```bash
rsync -av --progress /tmp/data.tar user@DEST_IP:/tmp/
```

---

### Problem 4 — rsync Permission Denied on /tmp

**Symptom**: rsync failed with "Permission denied" when trying to write to `/tmp/` on destination VM.

**Root cause**: A previous failed wget attempt had created 0-byte files in `/tmp/` owned by a different user (or root). rsync couldn't rename its temp files over them.

**Solution**: Delete the stale files on the destination first:

```bash
# On destination VM
sudo rm -f /tmp/*.tar /tmp/*.tar.gz
```

Then re-run rsync.

---

### Problem 5 — SSH Key Authorization Between VMs

**Symptom**: rsync from old VM to new VM fails — "Permission denied (publickey)".

**Root cause**: Public key added to wrong user's authorized_keys, wrong file permissions, or wrong ownership on `.ssh/` directory.

**Solution**: Follow this exact sequence:

```bash
# On old VM (source)
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -N ""
cat ~/.ssh/id_ed25519.pub
# Copy the full line output

# On new VM (as sudo user, e.g. photogeniusai)
sudo mkdir -p /home/amareshsingh2005/.ssh
echo "FULL_PUBLIC_KEY_LINE" | sudo tee -a /home/amareshsingh2005/.ssh/authorized_keys
sudo chown -R amareshsingh2005:amareshsingh2005 /home/amareshsingh2005/.ssh
sudo chmod 700 /home/amareshsingh2005/.ssh
sudo chmod 600 /home/amareshsingh2005/.ssh/authorized_keys

# Test
ssh -i ~/.ssh/id_ed25519 amareshsingh2005@NEW_VM_IP "echo OK"
```

---

### Problem 6 — gzip Compression Too Slow for Large Files

**Symptom**: `tar -czf /tmp/indexes.tar.gz books/indexes/` (16GB) was estimated to take 20+ minutes.

**Root cause**: Gzip is CPU-bound and compresses at ~100-200 MB/s. GCP same-region network is 300-400 MB/s — faster than compression speed.

**Solution**: Use tar WITHOUT compression:

```bash
# Slow (with gzip)
tar -czf /tmp/indexes.tar.gz books/indexes/

# Fast (no compression) — recommended
tar -cf /tmp/indexes.tar books/indexes/
```

The uncompressed file is larger but transfers faster because the network outpaces gzip.

---

### Problem 7 — Permission Denied When Extracting tar

**Symptom**: `tar -xf /tmp/indexes.tar` fails with "Cannot open: Permission denied".

**Root cause**: The destination path (`/home/amareshsingh2005/`) is owned by `amareshsingh2005`, but you're running tar as `photogeniusai` (the sudo account).

**Solution**: Use sudo for extraction, then fix ownership:

```bash
sudo tar -xf /tmp/indexes.tar -C /home/amareshsingh2005/
sudo chown -R amareshsingh2005:amareshsingh2005 /home/amareshsingh2005/books/
```

---

### Problem 8 — Home Directory Owned by Wrong User

**Symptom**: `amareshsingh2005` can't access files in their own home directory.

**Root cause**: When `photogeniusai` (sudo user) creates directories or files under `/home/amareshsingh2005/`, those files are owned by `photogeniusai`.

**Solution**: Fix ownership recursively:

```bash
sudo chown -R amareshsingh2005:amareshsingh2005 /home/amareshsingh2005/
```

Run this after any sudo operation that creates files in another user's home directory.

---

### Problem 9 — GEMINI_API_KEY Not Accessible to systemd Service

**Symptom**: API starts but Gemini calls fail with authentication errors. Key exists in `~/.bashrc` but service can't see it.

**Root cause**: systemd services don't source `.bashrc` or `.profile`. Environment variables set there are invisible to services.

**Solution**: Add the key to `/etc/environment` (system-wide, read by all processes including systemd):

```bash
sudo bash -c 'echo "GEMINI_API_KEY=your_key_here" >> /etc/environment'
```

Add `EnvironmentFile=/etc/environment` to the `[Service]` section of the systemd unit file.

Verify the service can see it:

```bash
sudo systemctl show brahm-api --property=Environment
```

---

### Problem 10 — Certbot Timeout (Port 80 Not Open)

**Symptom**: `certbot --nginx -d brahmasmi.bimoraai.com` times out during ACME HTTP challenge.

**Root cause**: No firewall rule allowing inbound TCP:80. Let's Encrypt can't reach the server to validate domain ownership.

**Solution**: Create firewall rule before running certbot:

```bash
# From Cloud Shell
gcloud compute firewall-rules create allow-http \
  --project=PROJECT_ID \
  --allow=tcp:80,tcp:443 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=http-server
```

The VM must have the `http-server` network tag (set during creation with `--tags=http-server,https-server`).

---

### Problem 11 — gcloud Commands Fail Inside VM

**Symptom**: Running `gcloud compute ssh ...` or `gcloud compute firewall-rules create ...` from inside a VM fails with "insufficient authentication scopes" or "credentials error".

**Root cause**: The VM's service account may not have the required IAM scopes for gcloud API calls, and the credentials context is different.

**Solution**: All `gcloud` commands must be run from **Cloud Shell** (browser), never from inside a VM.

Cloud Shell has your account credentials automatically. VMs use service account credentials with limited scopes by default.

---

### Problem 12 — Two VMs with Same Name in Different Projects

**Symptom**: Multiple terminal tabs all showing `brahm` in the prompt — impossible to tell which is old vs new VM.

**Root cause**: Both VMs were named "brahm" and the hostnames looked identical.

**Solution**: Check the OS user in the shell prompt:

- Old VM: `amareshsingh2005@brahm:~$`
- New VM: `photogeniusai@brahm:~$` (or the other user after su)

Alternatively, rename the new VM during creation to something distinguishable (e.g., `brahm-v2`), then rename back after migration completes.

---

## Quick Reference Commands

### Transfer a Large Directory

```bash
# On source VM
tar -cf /tmp/FOLDER_NAME.tar ~/books/FOLDER_NAME/
rsync -av --progress /tmp/FOLDER_NAME.tar USER@DEST_IP:/tmp/

# On destination VM
sudo tar -xf /tmp/FOLDER_NAME.tar -C /home/USER/
sudo chown -R USER:USER /home/USER/books/
sudo rm -f /tmp/FOLDER_NAME.tar
```

### Service Management

```bash
# Stop service (before heavy SSH work)
sudo systemctl stop brahm-api && sudo systemctl disable brahm-api

# Start service (after work is done)
sudo systemctl enable brahm-api && sudo systemctl start brahm-api

# Check service status
sudo systemctl status brahm-api

# View live logs
sudo journalctl -fu brahm-api
```

### Check What's Using RAM

```bash
free -h
htop
# Or check specific process
ps aux --sort=-%mem | head -20
```

### Fix Permissions

```bash
# Fix home directory ownership
sudo chown -R amareshsingh2005:amareshsingh2005 /home/amareshsingh2005/

# Fix .ssh permissions
sudo chmod 700 /home/amareshsingh2005/.ssh
sudo chmod 600 /home/amareshsingh2005/.ssh/authorized_keys
```

### Verify API is Running

```bash
curl http://localhost:8000/api/health
curl https://brahmasmi.bimoraai.com/api/health
```

---

## Key Lessons Learned

1. **GPU is not needed.** Gemini API handles all LLM inference. CPU handles sentence-transformer embeddings and reranking just fine. GPU VMs are expensive, frequently exhausted, and unnecessary for this stack.

2. **RAM is the real bottleneck.** BM25 cache (5.4GB) + documents.jsonl (5.1GB) + FAISS index + model weights = ~20-25GB peak. 64GB RAM has comfortable headroom.

3. **Same-region GCP transfers are very fast.** 200-400 MB/s is achievable. Skip compression — raw tar + rsync is the fastest method.

4. **Always disable the API service during heavy operations.** BM25 loading causes memory pressure that crashes the Guest Agent, making SSH unavailable. Keep the service off while doing transfers, certbot, or config changes.

5. **Never use `--reload` with uvicorn.** It breaks SSE streaming for the AI chat feature.

6. **systemd reads `/etc/environment`, not `.bashrc`.** All env vars needed by the service (especially `GEMINI_API_KEY`) must go in `/etc/environment`.

7. **gcloud commands belong in Cloud Shell.** Inside-VM gcloud calls frequently fail due to missing authentication scopes.

8. **Use sudo + chown after every cross-user file operation.** Whenever photogeniusai (sudo user) creates or extracts files into amareshsingh2005's home, fix ownership immediately.

9. **Port 80 must be open before certbot.** Create the firewall rule before attempting SSL certificate issuance.

10. **tar without gzip for large files.** For files >5GB on same-region GCP network, uncompressed tar transfers faster than gzip-compressed tar.

---

## Estimated Migration Timeline

| Task | Time Estimate |
|------|---------------|
| VM creation + firewall | 5 min |
| Package installation | 10 min |
| Python venv + pip install | 15 min |
| SSH key setup between VMs | 5 min |
| Data transfer (indexes 16GB) | ~5 min at 300+ MB/s |
| Data transfer (raw PDFs 36GB) | ~15 min at 300+ MB/s |
| Data transfer (remaining data) | ~15 min |
| Ownership fixes | 2 min |
| Code clone + service setup | 5 min |
| Nginx config + SSL | 10 min |
| DNS update + propagation | 5-30 min |
| Frontend deploy | 5 min |
| Testing + verification | 15 min |
| **Total** | **~2 hours** |

---

*This guide was written based on the actual migration performed on 2026-03-22. Update this document if future migrations reveal new problems or better solutions.*
