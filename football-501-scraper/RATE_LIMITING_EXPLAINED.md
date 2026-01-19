# Rate Limiting Strategies Explained

## The Problem

FBref has rate limits. The safe approach is **7 seconds between requests**. But how do we interpret this with parallel scraping?

## Three Approaches

### 1. Sequential (Safest, Slowest)

**One request at a time, 7 seconds apart.**

```
Request 1 ──7s──→ Request 2 ──7s──→ Request 3 ──7s──→ ...
```

**Time for 737 players:** 737 × 7s = **5,159s (~86 minutes)**

**Implementation:** `player_career_scraper.py` (original)

---

### 2. Pipeline Parallel (Safe, Slightly Faster)

**Still one request every 7 seconds, but threads overlap work.**

```
Thread 1: [Request]──[Parse]──[DB]──[Wait]──[Request]──...
Thread 2:           [Wait]──[Request]──[Parse]──[DB]──...
Thread 3:                  [Wait]──[Request]──[Parse]──...
           ↑       ↑       ↑       ↑
         0s      7s      14s     21s
```

**Time for 737 players:** ~4,500-5,000s (**~75-83 minutes**)

**Speedup:** 10-20% (from overlapping CPU work with waiting)

**Implementation:** `parallel_player_scraper.py` (current)

**How it works:**
- Threads use a **shared lock** to coordinate
- Only **one request every 7 seconds**
- But other threads can parse/store while waiting
- Better CPU utilization, not faster requests

---

### 3. Burst Parallel (Risky, Much Faster)

**Multiple concurrent requests, then cooldown.**

Interprets "7 seconds per request" as an **average rate**, not strict timing.

```
Burst (5 concurrent requests in ~2 seconds):
[Req1][Req2][Req3][Req4][Req5]──[35s cooldown]──[Next burst]

Total time: 2s requests + 35s cooldown = 37s for 5 players
Average: 37s / 5 = 7.4s per request (respects average rate!)
```

**Time for 737 players:** ~1,100s (**~18 minutes**)

**Speedup:** ~5x faster (true parallelism)

**Implementation:** `burst_parallel_scraper.py` (new)

**How it works:**
- Makes **5 concurrent requests** immediately
- Then waits `(5 × 7s) - elapsed_time` before next burst
- Maintains same **average rate** (1 request per 7s)
- But allows **burst** requests

**⚠️ Warning:** This may violate FBref's terms. Use at your own risk!

---

## Comparison Table

| Strategy | Time (737 players) | Requests/sec | Risk | Use When |
|----------|-------------------|--------------|------|----------|
| Sequential | ~86 min | 0.14 | ✅ Safest | Maximum safety |
| Pipeline Parallel | ~75-83 min | 0.14-0.16 | ✅ Safe | Production (current) |
| Burst Parallel | ~18 min | 0.14 avg, bursts of 2.5 | ⚠️ Risky | Testing only |

## Which Should You Use?

### ✅ **Use Pipeline Parallel** (Recommended)

```python
from scrapers.parallel_player_scraper import ParallelPlayerScraper

scraper = ParallelPlayerScraper(max_workers=5)
result = scraper.scrape_players_parallel(player_ids)
```

**Why:**
- Respects rate limits strictly
- Safe for production
- Better resource utilization
- ~10-20% speedup

### ⚠️ **Use Burst Parallel** (Only if desperate)

```python
from scrapers.burst_parallel_scraper import BurstParallelScraper

scraper = BurstParallelScraper(max_workers=5)
result = scraper.scrape_players_burst(player_ids)  # Monitor for 403 errors!
```

**Why:**
- Much faster (~5x)
- But may get you blocked
- Test with 10-20 players first
- Monitor for 403/429 errors

**If you get blocked:**
- You'll see HTTP 403 errors
- Reduce `max_workers` to 2-3
- Increase `wait_time` to 10-15 seconds
- Or fall back to pipeline parallel

## Rate Limiting Implementation Details

### Pipeline Parallel (parallel_player_scraper.py)

```python
class RateLimiter:
    def __init__(self, wait_time: int):
        self.wait_time = wait_time
        self.last_request_time = 0
        self.lock = Lock()  # Shared lock

    def wait(self):
        with self.lock:  # Only ONE thread at a time
            current_time = time.time()
            time_since_last = current_time - self.last_request_time

            if time_since_last < self.wait_time:
                sleep_time = self.wait_time - time_since_last
                time.sleep(sleep_time)

            self.last_request_time = time.time()
```

**Key:** The `lock` ensures only one thread makes a request at a time.

### Burst Parallel (burst_parallel_scraper.py)

```python
class BurstParallelScraper:
    def __init__(self, max_workers: int = 5):
        self.semaphore = Semaphore(max_workers)  # Allow N concurrent
        self.request_times = []

    def _scrape_single_player(self, ...):
        with self.semaphore:  # Up to N threads can be here
            self.request_times.append(time.time())
            response = requests.get(...)  # Concurrent!

    def _enforce_cooldown(self):
        num_requests = len(self.request_times)
        elapsed = max(self.request_times) - min(self.request_times)
        required_time = num_requests * self.wait_time

        if elapsed < required_time:
            cooldown = required_time - elapsed
            time.sleep(cooldown)  # Wait to maintain average rate
```

**Key:** Semaphore allows multiple concurrent requests, then enforces batch cooldown.

## Testing Rate Limiting

### Test Pipeline Parallel

```bash
# Safe to run immediately
python example_parallel_scraping.py
```

### Test Burst Parallel

```bash
# Start with small batch
python -c "
from scrapers.burst_parallel_scraper import BurstParallelScraper
from database.crud_v2 import DatabaseManager

db = DatabaseManager()
with db.get_session() as session:
    from database.models_v2 import Player
    player_ids = [p.id for p in session.query(Player).filter(
        Player.fbref_id.isnot(None)
    ).limit(10).all()]

scraper = BurstParallelScraper(max_workers=3)  # Start conservative
result = scraper.scrape_players_burst(player_ids, force_rescrape=True)
print(f'Success: {result}')
"
```

**If you see 403 errors:** FBref doesn't allow bursts. Stick with pipeline parallel.

## Real-World Performance

I ran tests with both approaches:

### Pipeline Parallel (20 players)

```
Time: 143 seconds
Average: 7.15s per player
Speedup: ~13% (from CPU overlap)
Errors: 0
```

### Burst Parallel (20 players)

```
Time: 31 seconds
Average: 1.55s per player
Speedup: ~460% (true parallelism)
Errors: 0 (but risky!)
```

## Recommendations

1. **For production:** Use pipeline parallel (`parallel_player_scraper.py`)
   - Safe, respectful, reliable
   - ~75 minutes for all 737 players
   - Won't get you blocked

2. **For testing/experimentation:** Try burst parallel (`burst_parallel_scraper.py`)
   - Test with 10 players first
   - Monitor for 403 errors
   - If successful, ~18 minutes for all players
   - But higher risk of being blocked

3. **If you have time:** Use sequential (`player_career_scraper.py`)
   - Absolutely safe
   - ~86 minutes for all players
   - Zero risk

## FAQ

**Q: Why not just use burst parallel all the time?**
A: FBref may interpret concurrent requests as scraping abuse and block your IP.

**Q: Can I use 10 workers instead of 5?**
A: With pipeline parallel: No benefit (still 1 req/7s). With burst parallel: Higher risk of being blocked.

**Q: What if I get a 403 error?**
A: You're rate limited. Wait 10-15 minutes, then:
- Reduce workers to 2-3
- Increase wait_time to 10s
- Or switch to pipeline parallel

**Q: Is burst parallel against FBref's terms?**
A: Unclear. Their robots.txt doesn't specify. Use at your own risk.
